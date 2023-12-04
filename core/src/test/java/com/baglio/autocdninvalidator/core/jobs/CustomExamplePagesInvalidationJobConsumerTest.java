package com.baglio.autocdninvalidator.core.jobs;

import static junit.framework.Assert.assertNotNull;

import com.baglio.autocdninvalidator.core.service.ReadService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import com.day.cq.commons.Externalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class CustomExamplePagesInvalidationJobConsumerTest {
  @Mock private Externalizer externalizer;
  @Mock private ReadService readService;

  @Mock private UtilityService utilityService;
  private final CustomExamplePagesInvalidationJobConsumerHelper consumer =
      new CustomExamplePagesInvalidationJobConsumerHelper();

  @Test
  void testSimpleUseCases() {
    assertNotNull(consumer.getExternalizer());
    assertNotNull(consumer.getReadService());
    assertNotNull(consumer.getUtilityService());
  }

  class CustomExamplePagesInvalidationJobConsumerHelper extends CustomExamplePagesInvalidationJobConsumer {
    @Override
    Externalizer getExternalizer() {
      return externalizer;
    }

    @Override
    ReadService getReadService() {
      return readService;
    }

    @Override
    UtilityService getUtilityService() {
      return utilityService;
    }
  }
}
