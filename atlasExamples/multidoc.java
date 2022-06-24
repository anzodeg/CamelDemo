/*
 * Copyright (C) 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atlasmap.examples.camel.main;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

/**
 * An example for running AtlasMap data mapping through the camel-atlasmap component.
 */
public class Application extends RouteBuilder {

    //this is just the msg.java one with less complexity - executes code the exact same way
    @Override
    public void configure() throws Exception {
        from("timer:main?period=5000")
            .to("direct:order-producer")
            .to("direct:contact-producer")
            .to("atlasmap:atlasmap-mapping.adm")
            .to("direct:outcome-consumer");

        from("direct:order-producer")
            .setProperty("order-schema", simple("resource:classpath:data/order.json"))
            .log("-->; Order: [${exchangeProperty.order-schema}]");
        
        from("direct:contact-producer")
            .setProperty("contact-schema", simple("resource:classpath:data/contact.xml"))
            .log("-->; Contact: [${exchangeProperty.contact-schema}]");
        
        from("direct:outcome-consumer")
            .log("--< Outcome: [${body}]");
    }

    /**
     * The application entry point.
     * @param args args
     * @throws Exception unexpected error
     */
    public static void main(String args[]) throws Exception {
        Main camelMain = new Main();
        camelMain.configure().addRoutesBuilder(new Application());
        camelMain.run(args);
    }
}