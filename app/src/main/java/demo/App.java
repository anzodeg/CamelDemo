package demo;

import java.io.FileWriter;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.XMLParser;

public class App {
    public static void main(String[] args) throws Exception {
        runCamelRoute(2000);
    }

    public static void runCamelRoute(Integer time) throws Exception {
        final String PATH_TO_INPUT = "file:app/src/main/resources/hl7?noop=true";
        final String PATH_TO_INTERMEDIATE = "app/src/main/resources/intermediate/intermediateXML.xml";
        final String MAP_TEMPLATE_NAME = "atlasmap:atlasmap-mapping.adm";
        final String PATH_TO_OUTPUT = "app/src/main/resources/output/fhir.json";

        CamelContext context = new DefaultCamelContext();
        //not necessary?
        // context.getEndpoint("file:app/src/main/resources/atlasmap-mapping.adm");
        // context.getEndpoint("file:app/src/main/resources/message.hl7");
        // context.getEndpoint("file:app/src/main/resources/out.json");

        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from(PATH_TO_INPUT)
                    .convertBodyTo(String.class)
                    //not sure what next 2 lines do - runs fine without them, ask Venki
                    .unmarshal()
                    .hl7(false)
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            final Message message = exchange.getIn().getBody(Message.class);
                            XMLParser parser = new DefaultXMLParser();
                            String payload = parser.encode(message);
                            FileWriter fw = new FileWriter(PATH_TO_INTERMEDIATE);
                            fw.write(payload);
                            fw.close();
                            System.out.println("Wrote Intermediate XML");
                            exchange.getIn().setBody(payload);
                        }
                    })
                    .to(MAP_TEMPLATE_NAME)
                    .convertBodyTo(String.class)
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            String message = exchange.getIn().getBody(String.class);
                            FileWriter fw = new FileWriter(PATH_TO_OUTPUT);
                            fw.write(message);
                            fw.close();
                            System.out.println("Wrote Output");
                        }
                    });
            }
        });

        System.out.println("Started Route");
        context.start();
        Thread.sleep(time);
        context.stop();
        context.close();
        System.out.println("Finished Route");
    }
}
