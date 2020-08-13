/*               "Copyright 2020 Infosys Ltd.
               Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
               This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3" */
package com.infosys.wingspan.authoringkafkaconsumers;

import com.google.common.collect.Sets;
import com.infosys.wingspan.authoringkafkaconsumers.utils.Neo4jQueryHelpers;
import org.neo4j.driver.v1.*;

import java.util.concurrent.atomic.AtomicInteger;


public class Test {

    private static AtomicInteger atomicInteger = new AtomicInteger();

    public static void main(String[] args) throws Exception {

        Driver neo4jDriver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "neo4j1"));
        Session session = neo4jDriver.session();
        Transaction tx = session.beginTransaction();


        Neo4jQueryHelpers heplers = new Neo4jQueryHelpers();

        heplers.deleteNodes("Infosys",Sets.newHashSet("lex_123","lex_1234"),tx);

		tx.success();
        tx.close();
        session.close();
        neo4jDriver.close();



    }
}