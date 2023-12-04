package com.baglio.autocdninvalidator.core.listeners;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class DynamicResourceChangeListenerTest {

  private static final String CONTENT_MY_SITE = "/content/my-site";
  private static final String CONTENT_MY_SITE_EN_HOME = CONTENT_MY_SITE + "/en/home";

  @Mock private DynamicResourceChangeListener.Config config;

  @Mock private JobManager jobManager;
  @InjectMocks private DynamicResourceChangeListener dynamicResourceChangeListener;

  @BeforeEach
  public void setUp() {
    config = mock(DynamicResourceChangeListener.Config.class);
    when(config.resource_paths()).thenReturn(new String[] {CONTENT_MY_SITE});
    dynamicResourceChangeListener.activate(config);
  }

  @Test
  void testExecution() {
    // test filtering of paths
    List<ResourceChange> changes = new ArrayList<>();
    changes.add(new ResourceChange(ResourceChange.ChangeType.CHANGED, CONTENT_MY_SITE_EN_HOME, false));
    changes.add(new ResourceChange(ResourceChange.ChangeType.CHANGED, CONTENT_MY_SITE_EN_HOME, false));
    changes.add(new ResourceChange(ResourceChange.ChangeType.CHANGED, CONTENT_MY_SITE, false));
    changes.add(new ResourceChange(ResourceChange.ChangeType.CHANGED, "", false));
    changes.add(null);

    Set<String> changesPaths = dynamicResourceChangeListener.getChangesPaths(changes);

    assertNotNull(changesPaths);
    assertEquals(2, changesPaths.size());
    assertTrue(changesPaths.contains(CONTENT_MY_SITE));
    assertTrue(changesPaths.contains(CONTENT_MY_SITE_EN_HOME));

    // test execution
    when(config.isEnabled()).thenReturn(true);
    dynamicResourceChangeListener.activate(config);
    dynamicResourceChangeListener.onChange(changes);
    verify(jobManager, times(1)).addJob(any(), any());
  }
}
