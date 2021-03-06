/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.mongo.impl.types;

import com.mongodb.BasicDBObject;
import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.connections.mongo.api.MongoIdentifiableEntity;
import org.keycloak.connections.mongo.api.types.Mapper;
import org.keycloak.connections.mongo.api.types.MapperContext;
import org.keycloak.connections.mongo.api.types.MapperRegistry;
import org.keycloak.connections.mongo.impl.EntityInfo;
import org.keycloak.connections.mongo.impl.MongoStoreImpl;
import org.keycloak.models.utils.reflection.Property;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BasicDBObjectMapper<S> implements Mapper<BasicDBObject, S> {

    private static final Logger logger = Logger.getLogger(BasicDBObjectMapper.class);

    private final MongoStoreImpl mongoStoreImpl;
    private final MapperRegistry mapperRegistry;
    private final Class<S> expectedEntityType;

    public BasicDBObjectMapper(MongoStoreImpl mongoStoreImpl, MapperRegistry mapperRegistry, Class<S> expectedEntityType) {
        this.mongoStoreImpl = mongoStoreImpl;
        this.mapperRegistry = mapperRegistry;
        this.expectedEntityType = expectedEntityType;
    }

    @Override
    public S convertObject(MapperContext<BasicDBObject, S> context) {
        BasicDBObject dbObject = context.getObjectToConvert();
        if (dbObject == null) {
            return null;
        }

        EntityInfo entityInfo = mongoStoreImpl.getEntityInfo(expectedEntityType);

        S entity;
        try {
            entity = expectedEntityType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (String key : dbObject.keySet()) {
            Object value = dbObject.get(key);
            Property<Object> property;

            if ("_id".equals(key)) {
                // Current property is "id"
                if (entity instanceof MongoIdentifiableEntity) {
                    ((MongoIdentifiableEntity)entity).setId(value.toString());
                }

            } else if ((property = entityInfo.getPropertyByName(key)) != null) {
                // It's declared property with @DBField annotation
                setPropertyValue(entity, value, property);

            } else {
                // Show warning if it's unknown
                logger.warn("Property with key " + key + " not known for type " + expectedEntityType);
            }
        }

        return entity;
    }

    private void setPropertyValue(Object entity, Object valueFromDB, Property property) {
        if (valueFromDB == null) {
            property.setValue(entity, null);
            return;
        }

        MapperContext<Object, Object> context;

        Type type = property.getBaseType();

        // This can be the case when we have parameterized type (like "List<String>")
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Type[] genericTypeArguments = parameterized.getActualTypeArguments();

            List<Type> genericTypes = Arrays.asList(genericTypeArguments);

            Class<?> expectedReturnType = (Class<?>)parameterized.getRawType();
            context = new MapperContext<Object, Object>(valueFromDB, expectedReturnType, genericTypes);
        } else {
            Class<?> expectedReturnType = (Class<?>)type;
            // handle primitives
            expectedReturnType = Types.boxedClass(expectedReturnType);
            context = new MapperContext<Object, Object>(valueFromDB, expectedReturnType, null);
        }

        Object appObject = mapperRegistry.convertDBObjectToApplicationObject(context);

        if (Types.boxedClass(property.getJavaClass()).isAssignableFrom(appObject.getClass())) {
            property.setValue(entity, appObject);
        } else {
            throw new IllegalStateException("Converted object " + appObject + " is not of type " +  context.getExpectedReturnType() +
                    ". So can't be assigned as property " + property.getName() + " of " + entity.getClass());
        }
    }

    @Override
    public Class<? extends BasicDBObject> getTypeOfObjectToConvert() {
        return BasicDBObject.class;
    }

    @Override
    public Class<S> getExpectedReturnType() {
        return expectedEntityType;
    }
}
