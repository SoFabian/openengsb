package org.openengsb.core.endpoints;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.openengsb.core.model.MethodCall;
import org.openengsb.core.model.ReturnValue;
import org.openengsb.core.transformation.Transformer;
import org.openengsb.util.serialization.SerializationException;

public class OpenEngSBEndpoint extends ProviderEndpoint {

    public OpenEngSBEndpoint() {
    }

    public OpenEngSBEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public OpenEngSBEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    // extend the visibility of this method from protected to public
    @Override
    public void sendSync(MessageExchange me) throws MessagingException {
        super.sendSync(me);
    }

    // extend the visibility of this method from protected to public
    @Override
    public void send(MessageExchange me) throws MessagingException {
        super.send(me);
    }

    protected MethodCall toMethodCall(Source source) throws SerializationException, TransformerException {
        return Transformer.toMethodCall(new SourceTransformer().toString(source));
    }

    protected ReturnValue toReturnValue(Source source) throws SerializationException, TransformerException {
        return Transformer.toReturnValue(new SourceTransformer().toString(source));
    }

    protected Source toSource(ReturnValue returnValue) throws SerializationException {
        return new StringSource(Transformer.toXml(returnValue));
    }

    protected Source toSource(MethodCall methodCall) throws SerializationException {
        return new StringSource(Transformer.toXml(methodCall));
    }

    protected String getContextId(NormalizedMessage in) {
        return (String) in.getProperty("contextId");
    }
}