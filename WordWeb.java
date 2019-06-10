/**
 * Created by: Arudrra Krishnan
 * Copyright 2019 Arudrra Krishnan
 */

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.View;


public class WordWeb {


    public static void main(String[] args) {
        //declaring filereading variables
        File inputFile = new File("Textfile.txt");
        HashMap<String,Subject> subjects = new HashMap<>();
        //Creating Graph for Display purposes
        Graph graph = new SingleGraph("Word Web");
        graph.setStrict(false);
        graph.setAutoCreate(true);
        //Strings for input and output with files
        String text = "";

        //Read the textfile into text
        try {
            byte[] inputBytes = Files.readAllBytes(inputFile.toPath());
            text = new String(inputBytes);
            //System.out.println(text);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //parsed through the text and annotated triples
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, depparse, natlog, openie");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        //assuming sentences are annotated
        if (sentences != null) {
            // parse through all the sentences in the list
            for (CoreMap sentence : sentences) {
                Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
                if (triples != null) {
                    //parse through all the triples
                    for (RelationTriple triple : triples) {
                        //add the subject to a hashmap of subject
                        for (CoreLabel s : triple.subject) {
                            Subject subject;
                            if (subjects.containsKey(s.toString())) {
                                subject = subjects.get(s.toString());
                            } else {
                                subject = new Subject(s.toString());
                            }
                            //add the predicate to to a hashmap of predicates in the subject
                            for (CoreLabel p : triple.relation) {
                                Predicate predicate;
                                if (subject.getPredicates().containsKey(p.toString())) {
                                    predicate = subject.getPredicates().get(p.toString());
                                } else {
                                    predicate = new Predicate(p.toString());
                                }
                                //add the object to a hashmap of objects in the predicate
                                for (CoreLabel o : triple.object) {
                                    Object object;
                                    if (predicate.getObjects().containsKey(o.toString())) {
                                        object = predicate.getObjects().get(o.toString());
                                    } else {
                                        object = new Object(o.toString());
                                    }
                                    predicate.addObject(object);
                                }
                                subject.addPredicate(predicate);
                            }
                            subjects.put(subject.getSubject(), subject);
                        }
                    }
                }
            }
        }

        //calculating the colors for the graph
        //colors are based off the number of links relative to other objecst of the same class
        int subjectColorMax = 0;
        int linkColorMax = 0;
        for (Subject s : subjects.values()) {
            if (s.getNumLinks() > subjectColorMax) {
                subjectColorMax = s.getNumLinks();
            }
            if (s.getPredicates().size() > linkColorMax) {
                linkColorMax = s.getPredicates().size();
            }
        }

        //creating the nodes and links for the graph
        int linkLabel = 0;
        for (Subject s : subjects.values()) {
            graph.addNode(s.getSubject());
            graph.getNode(s.getSubject()).setAttribute("ui.label", s.getSubject().substring(0, s.getSubject().indexOf('-')));
            graph.getNode(s.getSubject()).addAttribute("ui.style", "fill-color: rgb(" +(int)(255*((double)s.getNumLinks()/(double)subjectColorMax)) +",0,0);");
            //System.out.println("Subject " +s.getSubject());
            for (Predicate p : s.getPredicates().values()) {
                //System.out.println("Predicate " +p.getPredicate());
                String object = "";
                for (Object o : p.getObjects().values()) {
                    object += o.getObject() +" ";
                    linkLabel++;
                    graph.addNode(o.getObject());
                    graph.getNode(o.getObject()).setAttribute("ui.label", o.getObject().substring(0, o.getObject().indexOf('-')));
                    //graph.getNode(o.getObject()).addAttribute("ui.style", "fill-color: rgb(130,155,155);");
                    graph.addEdge(""+linkLabel, s.getSubject(), o.getObject());
                    if (graph.getEdge(""+linkLabel) == null) {
                        graph.removeNode(o.getObject());
                        graph.removeEdge(""+linkLabel);
                    } else {
                        graph.getEdge("" + linkLabel).setAttribute("ui.label", p.getPredicate().substring(0, p.getPredicate().indexOf('-')));
                        graph.getEdge("" + linkLabel).addAttribute("ui.style", "fill-color: rgb(" +(int)(255*((double)s.getPredicates().size()/(double)linkColorMax)) +",0," +(int)(255*((double)s.getPredicates().size()/(double)linkColorMax)) +");");

                    }
                }
                //System.out.println("Object " +object);

            }
        }

        //setting zoom features
        //zoom in by pressing the 1-key
        //zoom out by pressing the 2-key
        //move the center using the arrow keys
        Viewer viewer = graph.display();
        View view = viewer.getDefaultView();
        view.getCamera().setViewPercent(1.0);
        view.addKeyListener(new KeyListener() {
            double zoom = 1.0;
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (e.getKeyCode() == KeyEvent.VK_1 && zoom > 0.01) {
                    if (zoom > 0.2) {
                        zoom -= 0.1;
                    } else {
                        zoom -= 0.01;
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_2 && zoom < 1.0){
                    if (zoom > 0.2) {
                        zoom += 0.1;
                    } else {
                        zoom += 0.01;
                    }                }
                view.getCamera().setViewPercent(zoom);
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });



    }

    //subject, predicate, and object classes for the hashmap structure
    public static class Subject {
        HashMap<String,Predicate> predicates = new HashMap<>();
        String subject;
        public Subject(String subject) {
            this.subject = subject;
        }
        public HashMap<String, Predicate> getPredicates() {
            return predicates;
        }
        public void addPredicate(Predicate predicate) {
            predicates.put(predicate.getPredicate(), predicate);
        }
        public String getSubject() {
            return subject;
        }
        public int getNumLinks() {
            int numObjects = 0;
            for (Predicate p : predicates.values()) {
                numObjects += p.getObjects().size();
            }
            return  predicates.size() + numObjects;
        }
    }

    public static class Predicate {
        HashMap<String,Object> objects= new HashMap<>();
        String predicate;
        public Predicate(String predicate) {
            this.predicate = predicate;
        }
        public HashMap<String, Object> getObjects() {
            return objects;
        }
        public void addObject(Object object) {
            objects.put(object.getObject(), object);
        }
        public String getPredicate() {
            return predicate;
        }

    }

    public static class Object {
        String object;
        public Object(String object) {
            this.object = object;
        }
        public String getObject() {
            return object;
        }
    }


}
