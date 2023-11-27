package com.baglio.autocdninvalidator.core.jobs;

import com.baglio.autocdninvalidator.core.service.ReadService;
import com.baglio.autocdninvalidator.core.service.UtilityService;
import com.day.cq.commons.Externalizer;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = JobConsumer.class, immediate = true)
@Designate(ocd = CustomExamplePagesInvalidationJobConsumer.Config.class)
public class CustomExamplePagesInvalidationJobConsumer extends EditorialAssetInvalidationJobConsumer {

  @Reference private Externalizer externalizer;
  @Reference private ReadService readService;
  @Reference private UtilityService utilityService;

  /**
   * Execute the job. If the job has been processed successfully, JobResult.OK should be returned. If the job has not
   * been processed completely, but might be rescheduled JobResult.FAILED should be returned. If the job processing
   * failed and should not be rescheduled, JobResult.CANCEL should be returned.
   *
   * @param job The job
   * @return The job result
   */
  @Override
  public JobConsumer.JobResult process(final Job job) {
    return super.process(job);
  }

  /** {@inheritDoc} */
  @Override
  Externalizer getExternalizer() {
    return externalizer;
  }

  /** {@inheritDoc} */
  @Override
  ReadService getReadService() {
    return readService;
  }

  /** {@inheritDoc} */
  @Override
  UtilityService getUtilityService() {
    return utilityService;
  }

  @ObjectClassDefinition(name = "Auto CDN Invalidator - Job Consumer - Website Specific")
  public interface Config extends EditorialAssetInvalidationJobConsumer.Config {}
}
