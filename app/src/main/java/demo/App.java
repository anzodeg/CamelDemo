package demo;

import java.io.FileWriter;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.XMLParser;

public class App {
    public static void main(String[] args) throws Exception {
        runCamelRoute(5000);
    }
    
    /**
     * Performs conversion from hl7 to fhir json
     * 
     * @param time how long to run the camel route for - there is not a good way to
     *             terminate the route upon completion so we just use an arbitrary amount of time
     * @throws Exception
     */
    public static void runCamelRoute(Integer time) throws Exception {
        final String PATH_TO_INPUT = "file:app/src/main/resources/hl7?noop=true";
        final String PATH_TO_INTERMEDIATE = "app/src/main/resources/intermediate/intermediateXML.xml";
        final String MAP_TEMPLATE_NAME = "atlasmap:atlasmap-mapping.adm";
        final String PATH_TO_OUTPUT = "app/src/main/resources/output/fhir.json";

        CamelContext context = new DefaultCamelContext();

        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from(PATH_TO_INPUT)
                    .convertBodyTo(String.class)
                    .unmarshal()
                    .hl7(false) //checks if hl7 input is valid
                    .process(new Processor() {
                        //convert hl7 to XML for processing
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
                    .to(MAP_TEMPLATE_NAME) //run conversion
                    .convertBodyTo(String.class)
                    .process(new Processor() {
                        //store converted fhir json
                        public void process(Exchange exchange) throws Exception {
                            String message = exchange.getIn().getBody(String.class);
                            FileWriter fw = new FileWriter(PATH_TO_OUTPUT);
                            //add whitespace to json for readability
                            JsonElement json = JsonParser.parseString(message);
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            String prettyJson = gson.toJson(json);
                            fw.write(prettyJson);
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
