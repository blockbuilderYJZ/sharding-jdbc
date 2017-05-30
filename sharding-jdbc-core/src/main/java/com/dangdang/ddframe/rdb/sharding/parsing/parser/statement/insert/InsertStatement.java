/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.insert;

import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.constant.SQLType;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.Condition;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.GeneratedKey;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.Column;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLNumberExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLPlaceholderExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.AbstractSQLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.GeneratedKeyToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.ItemsToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.SQLToken;
import com.google.common.base.Optional;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Insert SQL语句对象.
 *
 * @author zhangliang
 */
@Getter
@Setter
public final class InsertStatement extends AbstractSQLStatement {
    
    private final Collection<Column> columns = new LinkedList<>();
    
    private GeneratedKey generatedKey;
    
    private int columnsListLastPosition;
    
    private int valuesListLastPosition;
    
    public InsertStatement() {
        super(SQLType.INSERT);
    }
    
    /**
     * 追加自增主键标记对象.
     *
     * @param shardingRule 分片规则
     * @param parametersSize 参数个数
     */
    public void appendGenerateKeyToken(final ShardingRule shardingRule, final int parametersSize) {
        if (null != generatedKey) {
            return;
        }
        Optional<TableRule> tableRule = shardingRule.tryFindTableRule(getTables().get(0).getName());
        if (!tableRule.isPresent()) {
            return;
        }
        Optional<GeneratedKeyToken> generatedKeysToken = findGeneratedKeyToken();
        if (!generatedKeysToken.isPresent()) {
            return;
        }
        ItemsToken valuesToken = new ItemsToken(generatedKeysToken.get().getBeginPosition());
        if (0 == parametersSize) {
            appendGenerateKeyToken(shardingRule, tableRule.get(), valuesToken);
        } else {
            appendGenerateKeyToken(shardingRule, tableRule.get(), valuesToken, parametersSize);
        }
        getSqlTokens().remove(generatedKeysToken.get());
        getSqlTokens().add(valuesToken);
    }
    
    private void appendGenerateKeyToken(final ShardingRule shardingRule, final TableRule tableRule, final ItemsToken valuesToken) {
        Number generatedKey = shardingRule.generateKey(tableRule.getLogicTable());
        valuesToken.getItems().add(generatedKey.toString());
        addCondition(shardingRule, new Column(tableRule.getGenerateKeyColumn(), tableRule.getLogicTable()), new SQLNumberExpression(generatedKey));
        this.generatedKey = new GeneratedKey(tableRule.getLogicTable(), -1, generatedKey);
    }
    
    private void appendGenerateKeyToken(final ShardingRule shardingRule, final TableRule tableRule, final ItemsToken valuesToken, final int parametersSize) {
        valuesToken.getItems().add("?");
        addCondition(shardingRule, new Column(tableRule.getGenerateKeyColumn(), tableRule.getLogicTable()), new SQLPlaceholderExpression(parametersSize));
        generatedKey = new GeneratedKey(tableRule.getGenerateKeyColumn(), parametersSize, null);
    }
    
    private Optional<GeneratedKeyToken> findGeneratedKeyToken() {
        for (SQLToken each : getSqlTokens()) {
            if (each instanceof GeneratedKeyToken) {
                return Optional.of((GeneratedKeyToken) each);
            }
        }
        return Optional.absent();
    }
    
    private void addCondition(final ShardingRule shardingRule, final Column column, final SQLExpression sqlExpr) {
        if (shardingRule.isShardingColumn(column)) {
            add(new Condition(column, sqlExpr));
        }
    }
}
