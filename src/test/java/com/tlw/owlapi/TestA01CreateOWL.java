package com.tlw.owlapi;

import org.junit.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapitools.builders.BuilderClass;
import org.semanticweb.owlapitools.builders.BuilderClassAssertion;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.semanticweb.owlapi.search.EntitySearcher.getAnnotationObjects;

public class TestA01CreateOWL {
    IRI win60Iri = IRI.create("http://www.winning.com.cn/win60");
    String win60PrefixName = ":";
    OWLDataFactory df = new OWLDataFactoryImpl();
    DefaultPrefixManager prefixManager = new DefaultPrefixManager();
    private static final int INDENT = 4;
    private final ReasonerFactory reasonerFactory = new ReasonerFactory();
    private final PrintStream out = System.out;

    OWLOntologyManager getManager(){
        OWLOntologyManager instance = new OWLOntologyManagerImpl(df, new ReentrantReadWriteLock());
        OWLOntologyFactory factory = new OWLOntologyFactoryImpl((o, id) -> new OWLOntologyImpl(o, id));
        instance.getOntologyFactories().set(factory);
        return instance;
    }

    @Test
    public void testPrefixManager(){
        // 前缀管理器获得
        prefixManager.prefixNames().forEach(e -> System.out.println(e + " is " + prefixManager.getPrefix(e)));
        System.out.println(prefixManager.getPrefix("owl:"));
    }

    @Test
    public void testPrefixManagerAdd(){
        // 测试获得前缀管理
        prefixManager.setPrefix(win60PrefixName, win60Iri.toString());
        System.out.println(prefixManager.getPrefix(win60PrefixName));
        System.out.println(prefixManager.getIRI(win60PrefixName));
    }

    @Test
    public void testCreateClass(){
        OWLClass owlClass = df.getOWLClass(":abc");
        System.out.println(owlClass);
    }

    @Test
    public void createOWL01() throws OWLOntologyCreationException, OWLRendererException {
        OWLOntologyManager manager = getManager();
        OWLOntology ontology = manager.createOntology(win60Iri);
        OWLClass classA = df.getOWLClass(IRI.create("A"));
        OWLClass classB = df.getOWLClass(IRI.create("B"));
        OWLClass classC = df.getOWLClass(IRI.create("C"));
        OWLAxiom aAxiom = df.getOWLSubClassOfAxiom(classA, df.getOWLThing());
        ontology.addAxiom(aAxiom);
        OWLAxiom abAxiom = df.getOWLSubClassOfAxiom(classB, classA);
        ontology.add(abAxiom);
        OWLAxiom bcAxiom = df.getOWLSubClassOfAxiom(classC, classB);
        ontology.add(bcAxiom);
//        OWLAxiom caAxiom = df.getOWLSubClassOfAxiom(classC, classA);
//        ontology.add(caAxiom);
//        ManchesterOWLSyntaxRenderer renderer = new ManchesterOWLSyntaxRenderer();
//        renderer.render(ontology, System.out);
        ReasonerFactory factory = new ReasonerFactory();
        OWLReasoner reasoner = factory.createReasoner(ontology);
        reasoner.getSubClasses(classA, false).forEach(c -> System.out.println(c));
//        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//        ontology.getClassesInSignature().forEach(sup -> {
//            reasoner.getSubClasses(sup, true).getFlattened().forEach(sub -> System.out.println(sup + " is parent of: " + sub ));
//        });
//        reasoner.subClasses(classA).forEach(c -> System.out.println(c));
//        System.out.println(reasoner.isConsistent());
//        System.out.println(reasoner.isEntailed(abAxiom, bcAxiom, caAxiom));

//        System.out.println("---");
//        renderer.render(ontology, System.out);
    }

    @Test
    public void test01() throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLOntology ontology = manager.createOntology();
        OWLClass classA = factory.getOWLClass("A");
        OWLClass classB = factory.getOWLClass("B");
        OWLClass classC = factory.getOWLClass("C");
        OWLAxiom aAxiom = factory.getOWLSubClassOfAxiom(classA, factory.getOWLThing());
        OWLAxiom abAxiom = factory.getOWLSubClassOfAxiom(classB, classA);
        OWLAxiom bcAxiom = factory.getOWLSubClassOfAxiom(classC, classB);
        ontology.add(aAxiom, abAxiom, bcAxiom);

        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
        printHierarchy(ontology, reasoner, classA, 0);
        /* Now print out any unsatisfiable classes */
        ontology.classesInSignature().filter(c -> !reasoner.isSatisfiable(c))
                .forEach(c -> out.println("XXX: " + labelFor(ontology, c)));
        reasoner.dispose();
    }

    private void printHierarchy(OWLOntology ontology, OWLReasoner reasoner, OWLClass clazz, int level) {
        /*
         * Only print satisfiable classes -- otherwise we end up with bottom everywhere
         */
        if (reasoner.isSatisfiable(clazz)) {
            for (int i = 0; i < level * INDENT; i++) {
                out.print(" ");
            }
            out.println(labelFor(ontology, clazz));
            /* Find the children and recurse */
            reasoner.getSubClasses(clazz, true).entities().filter(c -> !c.equals(clazz))
                    .forEach(c -> printHierarchy(ontology, reasoner, c, level + 1));
        }
    }

    private String labelFor(OWLOntology ontology, OWLClass clazz) {
        /*
         * Use a visitor to extract label annotations
         */
        LabelExtractor le = new LabelExtractor();
        getAnnotationObjects(clazz, ontology).forEach(a -> a.accept(le));
        /* Print out the label if there is one. If not, just use the class URI */
        String result = le.getResult();
        if (result != null) {
            return result;
        }
        return clazz.getIRI().toString();
    }
}
