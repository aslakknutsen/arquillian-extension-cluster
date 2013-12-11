package org.jboss.arquillian.extension.cluster;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
 * Does not automatically test the randomness, only manual verification.
 */
@RunWith(Arquillian.class)
public class ClusterRandomnessTestCase {

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
            .addClass(MyBean.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @Inject
    private MyBean bean;
    
    @Test
    public void shouldCall1() throws Exception {
        bean.call(1);
    }

    @Test
    public void shouldCall2() throws Exception {
        bean.call(2);
    }

    @Test
    public void shouldCall3() throws Exception {
        bean.call(3);
    }

    @Test
    public void shouldCall4() throws Exception {
        bean.call(4);
    }

    @Test
    public void shouldCall5() throws Exception {
        bean.call(5);
    }
   
    @Test
    public void shouldCall6() throws Exception {
        bean.call(6);
    }

}
