/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Score;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * @author Rizwan Idrees
 * @author Mohsin Husen
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Peter-Josef Meisch
 */
public class SimpleElasticsearchPersistentEntityTests {

	@Test
	public void shouldThrowExceptionGivenVersionPropertyIsNotLong() {
		// given
		TypeInformation typeInformation = ClassTypeInformation.from(EntityWithWrongVersionType.class);
		SimpleElasticsearchPersistentEntity<EntityWithWrongVersionType> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);
		// when
		assertThatThrownBy(() -> {
			SimpleElasticsearchPersistentProperty persistentProperty = createProperty(entity, "version");
		}).isInstanceOf(MappingException.class);
	}

	@Test
	public void shouldThrowExceptionGivenMultipleVersionPropertiesArePresent() {
		// given
		TypeInformation typeInformation = ClassTypeInformation.from(EntityWithMultipleVersionField.class);
		SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField> entity = new SimpleElasticsearchPersistentEntity<>(
				typeInformation);

		SimpleElasticsearchPersistentProperty persistentProperty1 = createProperty(entity, "version1");

		SimpleElasticsearchPersistentProperty persistentProperty2 = createProperty(entity, "version2");

		entity.addPersistentProperty(persistentProperty1);
		// when
		assertThatThrownBy(() -> {
			entity.addPersistentProperty(persistentProperty2);
		}).isInstanceOf(MappingException.class);
	}

	@Test // DATAES-462
	public void rejectsMultipleScoreProperties() {

		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();

		assertThatExceptionOfType(MappingException.class) //
				.isThrownBy(() -> context.getRequiredPersistentEntity(TwoScoreProperties.class)) //
				.withMessageContaining("first") //
				.withMessageContaining("second");
	}

	@Test
	void shouldFindPropertiesByMappedName() {

		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context
				.getRequiredPersistentEntity(FieldNameEntity.class);

		ElasticsearchPersistentProperty persistentProperty = persistentEntity
				.getPersistentPropertyWithFieldName("renamed-field");

		assertThat(persistentProperty).isNotNull();
		assertThat(persistentProperty.getName()).isEqualTo("renamedField");
		assertThat(persistentProperty.getFieldName()).isEqualTo("renamed-field");
	}

	private static SimpleElasticsearchPersistentProperty createProperty(SimpleElasticsearchPersistentEntity<?> entity,
			String field) {

		TypeInformation<?> type = entity.getTypeInformation();
		Property property = Property.of(type, ReflectionUtils.findField(entity.getType(), field));
		return new SimpleElasticsearchPersistentProperty(property, entity, SimpleTypeHolder.DEFAULT);

	}

	private class EntityWithWrongVersionType {

		@Version private String version;

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	private class EntityWithMultipleVersionField {

		@Version private Long version1;
		@Version private Long version2;

		public Long getVersion1() {
			return version1;
		}

		public void setVersion1(Long version1) {
			this.version1 = version1;
		}

		public Long getVersion2() {
			return version2;
		}

		public void setVersion2(Long version2) {
			this.version2 = version2;
		}
	}

	// DATAES-462

	static class TwoScoreProperties {

		@Score float first;
		@Score float second;
	}

	private static class FieldNameEntity {
		@Id private String id;
		@Field(name = "renamed-field") private String renamedField;
	}
}
