package org.jboss.arquillian.extension.cluster;

import java.util.List;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class RandomnessObserver {
    
    @Inject
    private Instance<ContainerRegistry> registryInst;
    
    /*
     * Given a group Container setup
     * When the DeploymentScenario is created
     * Then clone the single deployment and create one for each Container 
     */
    public void duplicateDeployments(@Observes DeploymentScenario scenario) {
        ContainerRegistry registry = registryInst.get();
        List<Deployment> scenarioDeployments = scenario.deployments();
        int scenarioDeploymentsSize = scenarioDeployments.size();
        for(int i = 0; i < scenarioDeploymentsSize; i++) {
            Deployment deployment = scenarioDeployments.get(i);
            DeploymentDescription source = deployment.getDescription();
            for(Container container : registry.getContainers()) {
                Container defaulted = registry.getContainer(source.getTarget());
                // Only duplicate if this is not the DEFAULT target
                if(container != defaulted) {
                    DeploymentDescription target = cloneToNewTarget(source, container);
                    target.setTarget(new TargetDescription(container.getName()));
                    scenario.addDeployment(target);
                }
            }
        }
    }

    private DeploymentDescription cloneToNewTarget(DeploymentDescription source, Container container) {
        DeploymentDescription target = new DeploymentDescription(source.getName() + "-" + container.getName(), source.getArchive().shallowCopy());
        target.setExpectedException(source.getExpectedException());
        target.setOrder(source.getOrder());
        target.setProtocol(source.getProtocol());
        return target;
    }
}