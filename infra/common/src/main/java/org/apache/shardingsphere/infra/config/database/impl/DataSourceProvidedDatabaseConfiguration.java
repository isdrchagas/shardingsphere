/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.config.database.impl;

import lombok.Getter;
import org.apache.shardingsphere.infra.config.database.DatabaseConfiguration;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.infra.datasource.pool.props.creator.DataSourcePoolPropertiesCreator;
import org.apache.shardingsphere.infra.datasource.pool.props.domain.DataSourcePoolProperties;
import org.apache.shardingsphere.infra.metadata.database.resource.StorageResource;
import org.apache.shardingsphere.infra.metadata.database.resource.node.StorageNode;
import org.apache.shardingsphere.infra.metadata.database.resource.node.StorageNodeAggregator;
import org.apache.shardingsphere.infra.metadata.database.resource.unit.StorageUnit;
import org.apache.shardingsphere.infra.metadata.database.resource.unit.StorageUnitNodeMapCreator;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Data source provided database configuration.
 */
@Getter
public final class DataSourceProvidedDatabaseConfiguration implements DatabaseConfiguration {
    
    private final Collection<RuleConfiguration> ruleConfigurations;
    
    private final Map<String, StorageUnit> storageUnits;
    
    private final StorageResource storageResource;
    
    public DataSourceProvidedDatabaseConfiguration(final Map<String, DataSource> dataSources, final Collection<RuleConfiguration> ruleConfigs) {
        this.ruleConfigurations = ruleConfigs;
        Map<String, StorageNode> storageUnitNodeMap = dataSources.keySet().stream()
                .collect(Collectors.toMap(each -> each, StorageNode::new, (oldValue, currentValue) -> oldValue, LinkedHashMap::new));
        Map<StorageNode, DataSource> storageNodeDataSources = StorageNodeAggregator.aggregateDataSources(dataSources);
        Map<String, DataSourcePoolProperties> dataSourcePoolPropertiesMap = createDataSourcePoolPropertiesMap(dataSources);
        storageUnits = new LinkedHashMap<>(dataSourcePoolPropertiesMap.size(), 1F);
        for (Entry<String, DataSourcePoolProperties> entry : dataSourcePoolPropertiesMap.entrySet()) {
            String storageUnitName = entry.getKey();
            StorageNode storageNode = storageUnitNodeMap.get(storageUnitName);
            StorageUnit storageUnit = new StorageUnit(storageNode, dataSourcePoolPropertiesMap.get(storageUnitName), storageNodeDataSources.get(storageNode));
            storageUnits.put(storageUnitName, storageUnit);
        }
        storageResource = new StorageResource(storageNodeDataSources, storageUnitNodeMap);
    }
    
    public DataSourceProvidedDatabaseConfiguration(final StorageResource storageResource,
                                                   final Collection<RuleConfiguration> ruleConfigs, final Map<String, DataSourcePoolProperties> dataSourcePoolPropertiesMap) {
        this.storageResource = storageResource;
        this.ruleConfigurations = ruleConfigs;
        Map<String, StorageNode> storageUnitNodeMap = StorageUnitNodeMapCreator.create(dataSourcePoolPropertiesMap);
        Map<StorageNode, DataSource> storageNodeDataSources = storageResource.getDataSources();
        storageUnits = new LinkedHashMap<>(dataSourcePoolPropertiesMap.size(), 1F);
        for (Entry<String, DataSourcePoolProperties> entry : dataSourcePoolPropertiesMap.entrySet()) {
            String storageUnitName = entry.getKey();
            StorageNode storageNode = storageUnitNodeMap.get(storageUnitName);
            StorageUnit storageUnit = new StorageUnit(storageNode, dataSourcePoolPropertiesMap.get(storageUnitName), storageNodeDataSources.get(storageNode));
            storageUnits.put(storageUnitName, storageUnit);
        }
    }
    
    private Map<String, DataSourcePoolProperties> createDataSourcePoolPropertiesMap(final Map<String, DataSource> dataSources) {
        return dataSources.entrySet().stream().collect(Collectors
                .toMap(Entry::getKey, entry -> DataSourcePoolPropertiesCreator.create(entry.getValue()), (oldValue, currentValue) -> oldValue, LinkedHashMap::new));
    }
}
