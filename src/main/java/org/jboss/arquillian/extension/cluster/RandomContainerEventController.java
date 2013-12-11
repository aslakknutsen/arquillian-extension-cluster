package org.jboss.arquillian.extension.cluster;

import java.lang.reflect.Method;
import java.util.Random;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentTargetDescription;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.DeploymentContext;
import org.jboss.arquillian.container.spi.event.ContainerMultiControlEvent;
import org.jboss.arquillian.container.spi.event.DeployManagedDeployments;
import org.jboss.arquillian.container.spi.event.SetupContainers;
import org.jboss.arquillian.container.spi.event.StartClassContainers;
import org.jboss.arquillian.container.spi.event.StartSuiteContainers;
import org.jboss.arquillian.container.spi.event.StopClassContainers;
import org.jboss.arquillian.container.spi.event.StopManualContainers;
import org.jboss.arquillian.container.spi.event.StopSuiteContainers;
import org.jboss.arquillian.container.spi.event.UnDeployManagedDeployments;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.impl.client.deployment.event.GenerateDeployment;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.arquillian.test.spi.event.suite.Test;
import org.jboss.arquillian.test.spi.event.suite.TestEvent;

/*
 * Copy of ContainerEventController with added feature of randomly choosing the Target to use.
 * Change can be found in locateDeployment()
 */
public class RandomContainerEventController
{
   @Inject 
   private Instance<ContainerContext> containerContext;

   @Inject 
   private Instance<DeploymentContext> deploymentContext;

   @Inject 
   private Instance<ContainerRegistry> containerRegistry;

   @Inject 
   private Instance<DeploymentScenario> deploymentScenario;

   @Inject
   private Event<ContainerMultiControlEvent> container;

   @Inject
   private Event<GenerateDeployment> deployment;

   /*
    * Suite Level
    */
   public void execute(@Observes BeforeSuite event)
   {
      container.fire(new SetupContainers());
      container.fire(new StartSuiteContainers());
   }

   public void execute(@Observes AfterSuite event)
   {
      container.fire(new StopSuiteContainers());
   }

   /*
    * Class Level
    */
   public void execute(@Observes BeforeClass event)
   {
      container.fire(new StartClassContainers());
      deployment.fire(new GenerateDeployment(event.getTestClass()));
      container.fire(new DeployManagedDeployments());
   }

   public void execute(@Observes AfterClass event)
   {
      try
      {
         container.fire(new UnDeployManagedDeployments());
      }
      finally
      {
         container.fire(new StopManualContainers());
         container.fire(new StopClassContainers());
      }
   }

   /*
    * Test Level
    * 
    * Activate Container and Deployment context on Before / Test / After events
    */
   public void createBeforeContext(@Observes EventContext<Before> context) 
   {
      createContext(context);
   }

   public void createTestContext(@Observes EventContext<Test> context) 
   {
      createContext(context);
   }

   public void createAfterContext(@Observes EventContext<After> context) 
   {
      createContext(context);
   }

   private void createContext(EventContext<? extends TestEvent> context) 
   {
      try
      {
         lookup(context.getEvent().getTestMethod(), new Activate());
         context.proceed();
      }
      finally
      {
         lookup(context.getEvent().getTestMethod(), new DeActivate());
      }
   }

   /*
    * Internal Helpers needed to extract @OperatesOnDeployment from TestMethod.
    * 
    * TODO: This should not rely on direct Reflection, but rather access the metadata through some 
    * common metadata layer.
    */

   private void lookup(Method method, ResultCallback callback)
   {
      DeploymentTargetDescription deploymentTarget = locateDeployment(method);
      
      ContainerRegistry containerRegistry = this.containerRegistry.get();
      DeploymentScenario deploymentScenario = this.deploymentScenario.get();
      
      Deployment deployment = deploymentScenario.deployment(deploymentTarget);
      if(deployment == null && deploymentTarget != DeploymentTargetDescription.DEFAULT)
      {
         // trying to operate on a non existing DeploymentTarget (which is not the DEFAULT)
         throw new IllegalStateException(
               "No deployment found in " + DeploymentScenario.class.getSimpleName() + " for defined target: " + deploymentTarget.getName() + ". " + 
               "Please verify that the @" + OperateOnDeployment.class.getSimpleName() + " annotation on method " + method.getName() + " match a defined " +
               "@" + org.jboss.arquillian.container.test.api.Deployment.class.getSimpleName() + ".name");
      }
      if(deployment != null)
      {
         Container container = containerRegistry.getContainer(deployment.getDescription().getTarget());
         callback.call(container, deployment);
      }
   }

   private Random ran = new Random();

   private DeploymentTargetDescription locateDeployment(Method method)
   {
       ContainerRegistry reg = containerRegistry.get();
       int size = reg.getContainers().size();
       
       Container contianer = reg.getContainers().get(ran.nextInt(size));
       if(contianer.getContainerConfiguration().isDefault()) {
           return DeploymentTargetDescription.DEFAULT;
       }
       return new DeploymentTargetDescription(
           DeploymentTargetDescription.DEFAULT.getName() + "-" + contianer.getName());
   }

   private abstract class ResultCallback
   {
      abstract void call(Container container, Deployment deployment);
   }

   private class Activate extends ResultCallback
   {
      @Override
      void call(Container container, Deployment deployment)
      {
         containerContext.get().activate(container.getName());
         deploymentContext.get().activate(deployment);
      }
   }

   private class DeActivate extends ResultCallback
   {
      @Override
      void call(Container container, Deployment deployment)
      {
         containerContext.get().deactivate();
         deploymentContext.get().deactivate();
      }
   }
}
