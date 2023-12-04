package com.baglio.autocdninvalidator.core.listeners;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class ReplicationEventListenerTest {

  private static final String CONTENT_MY_SITE = "/content/my-site";
  private static final String CONTENT_MY_SITE_EN_HOME = CONTENT_MY_SITE + "/en/home/" + JcrConstants.JCR_CONTENT;

  @Mock private JobManager jobManager;
  @InjectMocks private ReplicationEventListener replicationEventListener;

  @BeforeEach
  public void setUp() {
    ReplicationEventListener.Config config = mock(ReplicationEventListener.Config.class);
    when(config.resource_paths()).thenReturn(new String[] {CONTENT_MY_SITE});
    when(config.isEnabled()).thenReturn(true);
    when(config.job_topic()).thenReturn("jobTopic");
    when(config.filter_regex()).thenReturn(CONTENT_MY_SITE + "/(?!.*adventures).*");
    replicationEventListener.activate(config);
  }

  @Test
  void testHandleEventShouldProcessPath() {
    Job job = mock(Job.class);

    lenient().when(jobManager.addJob(anyString(), anyMap())).thenReturn(job);

    boolean result = replicationEventListener.processPath(CONTENT_MY_SITE_EN_HOME);
    assertTrue(result);

    result = replicationEventListener.processPath("/content/my-site/us/en/adventures/test");
    assertFalse(result);
  }

  @Test
  void testHandleEventShouldSkipPath() {
    boolean result = replicationEventListener.processPath(CONTENT_MY_SITE_EN_HOME.replace("my", "other"));
    assertFalse(result);
  }
}
