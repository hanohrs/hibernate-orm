/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id.enhanced;

import java.util.function.Consumer;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceNamingStrategyTest {

	@Test
	public void testSequenceNameStandardStrategy() {
		verify( TestEntity.class, "TestEntity_SEQ" );
		verify( TestEntity.class, StandardNamingStrategy.STRATEGY_NAME, "TestEntity_SEQ" );
		verify( TestEntity.class, StandardNamingStrategy.class.getName(), "TestEntity_SEQ" );
	}

	@Test
	public void testSequenceNameHibernateSequenceStrategy() {
		verify( TestEntity.class, SingleNamingStrategy.STRATEGY_NAME, "hibernate_sequence" );
		verify( TestEntity.class, SingleNamingStrategy.class.getName(), "hibernate_sequence" );
	}

	@Test
	public void testSequenceNamePreferGeneratorNameStrategy() {
		verify( TestEntity.class, LegacyNamingStrategy.STRATEGY_NAME, "hibernate_sequence" );
		verify( TestEntity.class, LegacyNamingStrategy.class.getName(), "hibernate_sequence" );
	}

	@Test
	public void testNoGeneratorStandardStrategy() {
		verify( TestEntity2.class, "table_generator" );
		verify( TestEntity2.class, StandardNamingStrategy.STRATEGY_NAME, "table_generator" );
		verify( TestEntity2.class, StandardNamingStrategy.class.getName(), "table_generator" );
	}

	@Test
	public void testNoGeneratorHibernateSequenceStrategy() {
		verify( TestEntity2.class, SingleNamingStrategy.STRATEGY_NAME, "hibernate_sequence" );
		verify( TestEntity2.class, SingleNamingStrategy.class.getName(), "hibernate_sequence" );
	}

	@Test
	public void testNoGeneratorPreferGeneratorNameStrategy() {
		verify( TestEntity2.class, LegacyNamingStrategy.STRATEGY_NAME, "table_generator" );
		verify( TestEntity2.class, LegacyNamingStrategy.class.getName(), "table_generator" );
	}

	@Test
	public void testGeneratorWithoutSequenceNameStandardStrategy() {
		verify( TestEntity3.class, "table_generator" );
		verify( TestEntity3.class, StandardNamingStrategy.STRATEGY_NAME, "table_generator" );
		verify( TestEntity3.class, StandardNamingStrategy.class.getName(), "table_generator" );
	}

	@Test
	public void testGeneratorWithoutSequenceNameHibernateSequenceStrategy() {
		verify( TestEntity3.class, SingleNamingStrategy.STRATEGY_NAME, "hibernate_sequence" );
		verify( TestEntity3.class, SingleNamingStrategy.class.getName(), "hibernate_sequence" );
	}

	@Test
	public void testGeneratorWithoutSequenceNamePreferGeneratorNameStrategy() {
		verify( TestEntity3.class, LegacyNamingStrategy.STRATEGY_NAME, "table_generator" );
		verify( TestEntity3.class, LegacyNamingStrategy.class.getName(), "table_generator" );
	}

	@Test
	public void testGeneratorWithSequenceNameStandardStrategy() throws Exception {
		verify( TestEntity4.class, "test_sequence" );
		verify( TestEntity4.class, StandardNamingStrategy.STRATEGY_NAME, "test_sequence" );
		verify( TestEntity4.class, StandardNamingStrategy.class.getName(), "test_sequence" );
	}

	@Test
	public void testGeneratorWithSequenceNameHibernateSequenceStrategy() {
		verify( TestEntity4.class, SingleNamingStrategy.STRATEGY_NAME, "test_sequence" );
		verify( TestEntity4.class, SingleNamingStrategy.class.getName(), "test_sequence" );
	}

	@Test
	public void testGeneratorWithSequenceNamePreferGeneratorNameStrategy() throws Exception {
		verify( TestEntity4.class, LegacyNamingStrategy.STRATEGY_NAME, "test_sequence" );
		verify( TestEntity4.class, LegacyNamingStrategy.class.getName(), "test_sequence" );
	}

	private void verify(Class<?> entityType, String expectedName) {
		verify( entityType, null, expectedName );
	}

	private void verify(Class<?> entityType, String strategy, String expectedName) {
		withMetadata( entityType, strategy, (metadata) -> {
			final Namespace defaultNamespace = metadata.getDatabase().getDefaultNamespace();
			final Sequence sequence = defaultNamespace.locateSequence( Identifier.toIdentifier( expectedName ) );
			assertThat( sequence ).isNotNull();

			final PersistentClass entityBinding = metadata.getEntityBinding( entityType.getName() );
			final IdentifierGenerator generator = extractGenerator( entityBinding );
			assertThat( generator ).isInstanceOf( SequenceStyleGenerator.class );
			final SequenceStyleGenerator sequenceStyleGenerator = (SequenceStyleGenerator) generator;
			assertThat( sequenceStyleGenerator.getDatabaseStructure() ).isInstanceOf( SequenceStructure.class );
			final SequenceStructure sequenceStructure = (SequenceStructure) sequenceStyleGenerator.getDatabaseStructure();
			assertThat( sequenceStructure.getPhysicalName().getObjectName().getText() ).isEqualTo( expectedName );
		} );
	}

	private static void withMetadata(Class<?> entityClass, String namingStrategy, Consumer<MetadataImplementor> consumer) {
		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );

		if ( namingStrategy != null ) {
			ssrb.applySetting( AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY, namingStrategy );
		}

		try ( final ServiceRegistry ssr = ssrb.build() ) {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( entityClass );

			final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.validate();

			consumer.accept( metadata );
		}
	}

	private IdentifierGenerator extractGenerator(PersistentClass entityBinding) {
		return entityBinding.getIdentifier().createIdentifierGenerator( null, null, null );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue(generator = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity3")
	public static class TestEntity3 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@SequenceGenerator(name = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity4")
	public static class TestEntity4 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@SequenceGenerator(name = "table_generator", sequenceName = "test_sequence")
		private Long id;

		private String name;
	}

}
