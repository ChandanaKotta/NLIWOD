package org.aksw.hawk.querybuilding;

import java.util.Set;

import org.aksw.autosparql.commons.qald.Question;
import org.aksw.hawk.nlp.MutableTreeNode;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.ResultSet;

public class RecursiveSparqlQueryBuilder {
	Logger log = LoggerFactory.getLogger(RecursiveSparqlQueryBuilder.class);

	public Set<SPARQLQuery> start(SPARQLQueryBuilder sparqlQueryBuilder, Question q) {
		Set<SPARQLQuery> returnSet = Sets.newHashSet(new SPARQLQuery());
		Set<String> variableSet = Sets.newHashSet("?proj", "?const");
		try {
			MutableTreeNode tmp = q.tree.getRoot();
			recursion(returnSet, variableSet, tmp);

		} catch (CloneNotSupportedException e) {
			log.error("Exception while recursion", e);
		}

		return returnSet;
	}

	private void recursion(Set<SPARQLQuery> returnSet, Set<String> variableSet, MutableTreeNode tmp) throws CloneNotSupportedException {
		Set<SPARQLQuery> sb = Sets.newHashSet();

		// if no annotations maybe a CombinedNN
		if (!tmp.getAnnotations().isEmpty()) {
			for (SPARQLQuery query : returnSet) {
				for (String anno : tmp.getAnnotations()) {
					if (tmp.posTag.matches("VB(.)*")) {
						// FIXME variablen iterieren
						SPARQLQuery variant1 = ((SPARQLQuery) query.clone());
						variant1.addConstraint("?proj  <" + anno + "> ?const.");

						SPARQLQuery variant2 = ((SPARQLQuery) query.clone());
						variant2.addConstraint("?const <" + anno + "> ?proj.");

						SPARQLQuery variant3 = ((SPARQLQuery) query.clone());
						variant3.addConstraint("?const ?proot ?proj.");

						sb.add(variant1);
						sb.add(variant2);
						sb.add(variant3);
					} else if (tmp.posTag.matches("NN(.)*|WRB")) {
						// nn can be predicats, e.g. currency

						SPARQLQuery variant1 = ((SPARQLQuery) query.clone());
						variant1.addConstraint("?proj  <" + anno + "> ?const.");

						SPARQLQuery variant2 = ((SPARQLQuery) query.clone());
						variant2.addConstraint("?const <" + anno + "> ?proj.");

						SPARQLQuery variant3 = ((SPARQLQuery) query.clone());
						variant3.addConstraint("?const a <" + anno + ">.");

						SPARQLQuery variant4 = ((SPARQLQuery) query.clone());
						variant4.addConstraint("?proj a <" + anno + ">.");

						SPARQLQuery variant5 = ((SPARQLQuery) query.clone());

						// sb.add(variant1);
						sb.add(variant2);
						sb.add(variant3);
						sb.add(variant4);
						sb.add(variant5);
					} else if (tmp.posTag.matches("WP")) {
						SPARQLQuery variant1 = ((SPARQLQuery) query.clone());
						variant1.addConstraint("?const a <" + anno + ">.");

						SPARQLQuery variant2 = ((SPARQLQuery) query.clone());
						variant2.addConstraint("?proj a <" + anno + ">.");

						SPARQLQuery variant3 = ((SPARQLQuery) query.clone());

						sb.add(variant1);
						sb.add(variant2);
						sb.add(variant3);
					} else {
						log.error("Tmp: " + tmp.label + " pos: " + tmp.posTag);
					}
				}
			}
		} else {
			if (tmp.posTag.matches("CombinedNN|NNP(.)*|JJ|CD")) {
				/*
				 * fall back to full text for cases like "crown"->"The_Crown"
				 * which are not found yet by NED
				 */
				for (SPARQLQuery query : returnSet) {
					SPARQLQuery variant1 = (SPARQLQuery) query.clone();
					variant1.addFilterOverAbstractsContraint("?proj", tmp.label);

					SPARQLQuery variant2 = (SPARQLQuery) query.clone();
					variant2.addFilterOverAbstractsContraint("?const", tmp.label);

					sb.add(variant1);
					sb.add(variant2);
				}
			} else if (tmp.posTag.matches("VB(.)*")) {
				for (SPARQLQuery query : returnSet) {
					SPARQLQuery variant1 = (SPARQLQuery) query.clone();
					variant1.addFilterOverAbstractsContraint("?proj", tmp.label);

					SPARQLQuery variant2 = (SPARQLQuery) query.clone();
					variant2.addFilterOverAbstractsContraint("?const", tmp.label);

					SPARQLQuery variant3 = (SPARQLQuery) query.clone();

					sb.add(variant1);
					sb.add(variant2);
					sb.add(variant3);

				}
			} else if (tmp.posTag.matches("ADD")) {
				Set<String> origLabels = getOrigLabel(tmp.label);
				for (SPARQLQuery query : returnSet) {
					SPARQLQuery variant1 = (SPARQLQuery) query.clone();
					variant1.addConstraint("?proj ?pbridge <" + tmp.label + ">.");

					SPARQLQuery variant2 = (SPARQLQuery) query.clone();
					variant2.addFilter("?proj IN (<" + tmp.label + ">)");

					SPARQLQuery variant3 = (SPARQLQuery) query.clone();

					sb.add(variant1);
					sb.add(variant2);
					sb.add(variant3);
					/*
					 * TODO hack query for correct label of node ie Cleopatra
					 * can be undone when each ADD node knows is original label
					 */
					for (String origLabel : origLabels) {
						SPARQLQuery variant4 = (SPARQLQuery) query.clone();
						variant4.addFilterOverAbstractsContraint("?proj", origLabel);

						SPARQLQuery variant5 = (SPARQLQuery) query.clone();
						variant5.addFilterOverAbstractsContraint("?const", origLabel);
						sb.add(variant4);
						sb.add(variant5);
					}
				}
			} else if (tmp.posTag.matches("NN|NNS")) {
				for (SPARQLQuery query : returnSet) {
					SPARQLQuery variant1 = (SPARQLQuery) query.clone();
					variant1.addFilterOverAbstractsContraint("?proj", tmp.label);

					SPARQLQuery variant2 = (SPARQLQuery) query.clone();
					variant2.addFilterOverAbstractsContraint("?const", tmp.label);

					SPARQLQuery variant3 = (SPARQLQuery) query.clone();

					sb.add(variant1);
					sb.add(variant2);
					sb.add(variant3);
				}
			} else if (tmp.posTag.matches("WP")) {
				// for Who and What
				sb.addAll(returnSet);
			} else {
				log.error("Tmp: " + tmp.label + " pos: " + tmp.posTag);
			}
		}
		returnSet.clear();
		returnSet.addAll(sb);

		for (MutableTreeNode child : tmp.getChildren()) {
			recursion(returnSet, variableSet, child);
		}

	}

	private Set<String> getOrigLabel(String label) {
		Set<String> resultset = Sets.newHashSet();
		String query = "SELECT str(?proj) as ?proj WHERE { <" + label + "> <http://www.w3.org/2000/01/rdf-schema#label> ?proj. FILTER(langMatches( lang(?proj), \"EN\" ))}";
		try {
			QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://dbpedia.org/sparql");
			QueryExecution qe = qef.createQueryExecution(query);
			if (qe != null) {
				log.debug(query.toString());
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					resultset.add(results.next().get("proj").toString());
				}
			}
		} catch (Exception e) {
			log.error(query.toString(), e);
		}
		return resultset;
	}
}
