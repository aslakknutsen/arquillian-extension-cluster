package org.jboss.arquillian.extension.cluster;


import org.jboss.arquillian.container.test.impl.ContainerTestExtension;
import org.jboss.arquillian.container.test.impl.client.ContainerEventController;
import org.jboss.arquillian.core.spi.context.Context;

/*
 * Extension to run a Test Suite developed for a Single Container 
 * in a Clustered Container Group setup.
 * 
 * The Extension will automatically clone the single deployment
 * out to each Container in the group, and execute the individual @Test 
 * methods on different nodes randomly. 
 */
public class ClusterExtension extends ContainerTestExtension {

    public void register(ExtensionBuilder builder) {
        builder.observer(RandomnessObserver.class);
        super.register(new OverrideExtensionBuilder(builder));
    }
    
    public static class OverrideExtensionBuilder implements ExtensionBuilder {
        
        private ExtensionBuilder delegate;
        
        public OverrideExtensionBuilder(ExtensionBuilder delegate) {
            this.delegate = delegate;
        }

        public ExtensionBuilder observer(Class<?> handler) {
            if(handler == ContainerEventController.class) {
                delegate.observer(RandomContainerEventController.class);
            }
            else {
                delegate.observer(handler);
            }
            return this;
        }

        public <T> ExtensionBuilder service(Class<T> service, Class<? extends T> impl) {
            delegate.service(service, impl);
            return this;
        }

        public <T> ExtensionBuilder override(Class<T> service, Class<? extends T> oldServiceImpl,
            Class<? extends T> newServiceImpl) {
            delegate.override(service, oldServiceImpl, newServiceImpl);
            return this;
        }

        public ExtensionBuilder context(Class<? extends Context> context) {
            delegate.context(context);
            return this;
        }
    }

}
