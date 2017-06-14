/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 * 
 * GNU Lesser General Public License
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 * **************************************************************
 */
package EmergencyDesktopAgents.ontology;

import jade.content.onto.*;
import jade.content.schema.*;

public class Ontology extends jade.content.onto.Ontology implements Vocabulary {

	// The singleton instance of this ontology
	private static jade.content.onto.Ontology theInstance = new Ontology();

	public static jade.content.onto.Ontology getInstance() {
		return theInstance;
	}

	/**
	 * Constructor
	 */
	private Ontology() {
		//#PJAVA_EXCLUDE_BEGIN
		/*#J2ME_INCLUDE_BEGIN
		//#PJAVA_EXCLUDE_END
		super(ONTOLOGY_NAME, BasicOntology.getInstance(), null);

		try {
			add(new PredicateSchema(JOINED));
			add(new PredicateSchema(LEFT));
			add(new PredicateSchema(SPOKEN));
		//#PJAVA_EXCLUDE_BEGIN
		#J2ME_INCLUDE_END*/
		//#PJAVA_EXCLUDE_END
		//#J2ME_EXCLUDE_BEGIN
		super(ONTOLOGY_NAME, BasicOntology.getInstance(), new CFReflectiveIntrospector());

		try {
			add(new PredicateSchema(JOINED), Joined.class);
			add(new PredicateSchema(LEFT), EmergencyDesktopAgents.ontology.Left.class);
			add(new PredicateSchema(SPOKEN), EmergencyDesktopAgents.ontology.Spoken.class);
		//#J2ME_EXCLUDE_END
			PredicateSchema ps = (PredicateSchema) getSchema(JOINED);
			ps.add(JOINED_WHO, (ConceptSchema) getSchema(BasicOntology.AID), 1, ObjectSchema.UNLIMITED);

			ps = (PredicateSchema) getSchema(LEFT);
			ps.add(LEFT_WHO, (ConceptSchema) getSchema(BasicOntology.AID), 1, ObjectSchema.UNLIMITED);

			ps = (PredicateSchema) getSchema(SPOKEN);
			ps.add(SPOKEN_WHAT, (PrimitiveSchema) getSchema(BasicOntology.STRING));
		} catch (OntologyException oe) {
			oe.printStackTrace();
		}
	}

}
