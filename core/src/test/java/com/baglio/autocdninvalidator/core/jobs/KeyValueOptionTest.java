package com.baglio.autocdninvalidator.core.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KeyValueOptionTest {

  @Test
  void testGetKey() {
    EditorialAssetInvalidationJobConsumer.KeyValueOption option =
        new EditorialAssetInvalidationJobConsumer.KeyValueOption("myKey", "value");

    String key = option.getKey();

    assertEquals("myKey", key);
  }

  @Test
  void testGetKeyTrims() {
    EditorialAssetInvalidationJobConsumer.KeyValueOption option =
        new EditorialAssetInvalidationJobConsumer.KeyValueOption(" myKey ", "value");

    String key = option.getKey();

    assertEquals("myKey", key);
  }

  @Test
  void testGetValue() {
    EditorialAssetInvalidationJobConsumer.KeyValueOption option =
        new EditorialAssetInvalidationJobConsumer.KeyValueOption("key", "myValue");

    String value = option.getValue();

    assertEquals("myValue", value);
  }

  @Test
  void testGetValueTrims() {
    EditorialAssetInvalidationJobConsumer.KeyValueOption option =
        new EditorialAssetInvalidationJobConsumer.KeyValueOption("key", " myValue ");

    String value = option.getValue();

    assertEquals("myValue", value);
  }
}
