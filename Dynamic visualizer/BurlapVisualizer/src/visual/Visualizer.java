/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;


import Tree.StateNode;
import Tree.StateTree;
import burlap.oomdp.core.states.State;
import burlapcontroller.Reward;
import burlapcontroller.actions.CriteriaAction;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.DataSizeAction;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.visual.VisualItem;

import prefuse.visual.expression.InGroupPredicate;
import prefuse.render.EdgeRenderer;
import BurlapVisualizer.MyController;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.GroundedAction;
import burlapcontroller.Termination;
import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.util.Hashtable;
import java.util.Iterator;
import javax.swing.JPanel;
import prefuse.action.filter.FisheyeTreeFilter;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.data.Tree;
import prefuse.visual.NodeItem;


/**
 * This class is our handle the the visualizer as a whole.  After the state space has been selected this class takes over execution.
 * @author Justin Lewis
 */
public final class Visualizer {
    Tree graph; //this is the major data type of prefuse(contains the nodes and edges and other info)
    Visualization vis;
    Display d;
    StateTree tree;
     List<StateNode> nodeList;
    private DataDisplay dataDisplay;
    FinalControlListener tooltipHandler;
    List<String> allAtrribs;
    Reward reward;
    JFrame frame;
    MyController thisController;
    FinalControlListener mouse;
    List<VisualItem> edgeItems; //this is a list of the edge items from init to target state
    List<Edge> edges;
    List<Node> nodes;
    StateValueContainer stateValueContainer;
    ActionValueContainer actionValueContainer;
    Chart chart;
    List<ComputeState> computableStates;
    List<ComputeState> oldComputeStates;
    double degredation;
    ComputeState lastComputeState; //this acts as our computeState that is our currentState
    List<State> chosenStates;
    List<CriteriaAction> chosenActions;
    ActionList color;
    ActionList layout;
    ActionList repaint;
    List<List<State>> visibleStates = new ArrayList();
    List<List<Boolean>> undoStates = new ArrayList(); /////unimplemented at the moment
    List<List<CriteriaAction>> visibleActions = new ArrayList();
    List<State> temporaryHiddenStates = new ArrayList();
    List<StateNode> temporaryHiddentStateNodes = new ArrayList();
    List<State> dontComputeStates = new ArrayList();
    
    JPanel panel;
    
    int[] Originals_pallette;
    int[] s_pallette;
    int[] Originale_pallette;
    int[] e_pallette;
    
    DataColorAction fill;
    DataColorAction dcaEdges;
    DataColorAction arrowColor;
    
    
    
    /**
     * This function will close the {@link visual.DataDisplay} and this class.
     */
    public void closeWindows()
    {
        if(frame != null && dataDisplay != null)
        {
            frame.dispose();
            dataDisplay.close(); 
            chart.close();
            
        }

    }
    
    /**
     * This thread sets up the actual visualization with the fed in data.
     * <p>
     * When this function ends all that is left running is control listeners and prefuse threads.
     * There is nothing more we must do.
     * @param tree tree data structure you want to visualize.
     * @param allAttribs List of string of attributes each state has
     * @param actions List of every possible action in the MDP
     * @param controller The underlying controller for this MDP
     * @throws IOException
     * @throws ParseException 
     */
    public Visualizer(StateTree tree, List<String> allAttribs, List<CriteriaAction> actions, MyController controller, double degredation) throws IOException, ParseException
    {
        reward = controller.getRewardFunction();
        actionValueContainer = new ActionValueContainer(reward);
        stateValueContainer = new StateValueContainer(actionValueContainer);
        
        Originals_pallette = new int[4];
        Originals_pallette[0] = ColorLib.rgb(0,0,255);
        Originals_pallette[1] = ColorLib.rgb(255, 0, 0);
        Originals_pallette[2] = ColorLib.rgb(102,51,153);
        Originals_pallette[3] = ColorLib.rgb(255,105,180);
        
        Originale_pallette = new int[2];
        Originale_pallette[0] = ColorLib.rgb(0,255,0);
        Originale_pallette[1] = ColorLib.rgb(255,0,0);
        

        
        this.computableStates = new ArrayList();
        this.oldComputeStates = new ArrayList();
        this.chosenStates = new ArrayList();
        this.chosenStates.add(tree.initialState);
        this.chosenActions = new ArrayList();
        
        this.tree = tree;
        this.nodeList = this.tree.getNodes();
        this.allAtrribs = allAttribs;
        thisController = controller;
        this.degredation = degredation;
        panel = new JPanel();
        mouse = new FinalControlListener(reward);
       
        //this sets up the initial visualizer
        setUpData(true);
        setUpVisualization();
        
        //this is the only time these functions get called in the whole program
        setUpRenderers();
        setUpActions();
        setUpDisplay();
        chart = new Chart(stateValueContainer, actionValueContainer);
        mouse.setChart(chart);
}

    
    
    /**
     * This is the last function in the program that gets called that sets up the windows for viewing.
     */
    public void setUpDisplay()
    {
        mouse = new FinalControlListener(thisController.getRewardFunction()); //create listener
        dataDisplay = new DataDisplay(tree, this.allAtrribs, thisController,edgeItems, mouse); //set up the dataDisplay(mouse is needed for buttons)
        dataDisplay.setUpCharts("no action", null, null);//set charts up with nothing selected
        mouse.setDataDisplay(dataDisplay);//and let mouse have control of dataDisplay(To set up tables when user clicks)
        mouse.setVisualizer(this);
        mouse.currentState = (NodeItem) vis.getVisualItem("graph.nodes", nodes.get(0));
                        
        d = new Display(vis);
        d.setSize(900, 900);
        d.addControlListener(new DragControl());
        d.addControlListener(new PanControl());
        d.addControlListener(new ZoomControl());
        d.addControlListener(new WheelZoomControl());
        d.addControlListener(mouse); //controls when you click node or edge
        
//        panel.add(d);
        
        frame = new JFrame("Burlap Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(d);
        frame.pack();
        frame.setVisible(true);
        
        //these layouts were defined in setUpActions()
        vis.run("color");
        vis.run("layout");
    }
    

    
    
    /**
     * This function sets up the visualization with the given graph.
     * <p>
     * The visualizer in prefuse handles things like color rather than the raw data like the graph.
     */
    public void setUpVisualization()
    {       
        vis = new Visualization();

        Table n = graph.getNodeTable();
        Table e = graph.getEdgeTable();
        Graph g_new = new Graph(n, e, true); //this was required to give edges directions
        

        vis.add("graph", g_new);
        setItems();//now that vis has been created we can deal with items.
    }
    
    public void updateVisualization()
    {
//        vis.removeAction("color"); //the old color has been changed so remove old color
        vis.putAction("color", color); //and replace with new color
        vis.putAction("layout", layout);
        setItems(); //handles stuff like giveing stokes and the optimal path
        //run the actions thate were cancelled in setUpData
        vis.run("color"); 
        vis.run("layout");
    }
    
    
    /**
     * This handles how the nodes and edges are drawn in prefuse.
     * <p>
     * We gave some edges labels and all nodes a label.  We also made the nodes oval shapped which was handled in this function.
     */
    public void setUpRenderers()
    {
        FinalRenderer r = new FinalRenderer();
        EdgeRenderer er = new EdgeRenderer(Constants.EDGE_TYPE_LINE, prefuse.Constants.EDGE_ARROW_FORWARD);
       
        DefaultRendererFactory drf = new DefaultRendererFactory();
        drf.setDefaultRenderer(r);
        drf.setDefaultEdgeRenderer(er);
        

        
        // We now have to have a renderer for our decorators.
        LabelRenderer actionNameLabel = new LabelRenderer("ActionName");
        drf.add(new InGroupPredicate("ActionName"), actionNameLabel);
        drf.add(new InGroupPredicate("stateRewards"), new LabelRenderer("StateReward"));
        

    
        // -- Decorators responsible for both types of labels(edges and nodes)
        //edge labels
        final Schema DECORATOR_SCHEMA = PrefuseLib.getVisualItemSchema();
        DECORATOR_SCHEMA.setDefault(VisualItem.INTERACTIVE, false); 
        DECORATOR_SCHEMA.setDefault(VisualItem.TEXTCOLOR, 
                               ColorLib.rgb(0, 0, 0)); 
        DECORATOR_SCHEMA.setDefault(VisualItem.FONT, 
                               FontLib.getFont("Goblin One",12));
        
        //node labels
        final Schema DECSchema = PrefuseLib.getVisualItemSchema();
        DECSchema.setDefault(VisualItem.INTERACTIVE, false);
        DECSchema.setDefault(VisualItem.TEXTCOLOR, ColorLib.rgb(0, 200, 0));
        DECSchema.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma", 12));
        
        //add decorators to vis
        vis.addDecorators("ActionName", "graph.edges", DECORATOR_SCHEMA);
        vis.addDecorators("stateRewards", "graph.nodes", DECSchema);
        
      
        // This Factory will use the ShapeRenderer for all nodes.
        vis.setRendererFactory(drf);
    }
    
    
    
    
    /**
     * These actions define how color and size work in our visualization.
     */
    public void setUpActions()
    {
        //these palletes are used in the action colors.  They change based on what is visible
        setStatePallette(true);
        setEdgePallette(true);
        
        
        //this action handles how nodes are colored
        fill = new DataColorAction("graph.nodes", "nodeInfo", Constants.ORDINAL, VisualItem.FILLCOLOR, s_pallette);   
        fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,255, 0));
        
        //this action handles how edges are colored
        dcaEdges = new DataColorAction("graph.edges", "inPath",  Constants.NOMINAL, VisualItem.STROKECOLOR, e_pallette);
        dcaEdges.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 255, 0));
        arrowColor = new DataColorAction("graph.edges", "inPath",Constants.NOMINAL, VisualItem.FILLCOLOR, e_pallette);
        arrowColor.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 255, 0));
        //this action handles how thick edges are
        DataSizeAction edgeThick = new DataSizeAction("graph.edges", "weight");
        edgeThick.setMinimumSize(20); //strange how this affects thickness and not the edge renderer....but whatever
        
        
        //list of actions that define color...let it run indefinatly
        color = new ActionList(Activity.INFINITY);
        color.add(fill);
        color.add(dcaEdges);
        color.add(arrowColor);
        color.add(edgeThick);

        layout = new ActionList(Activity.INFINITY);

        NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout("graph", Constants.ORIENT_LEFT_RIGHT, 150, 100, 120);
        treeLayout.setLayoutAnchor(new Point2D.Double(25,300));
            
        //layout just contains the tree layout, decorators and a repaint that helps when we 
        //modify the graph in the future
        layout.add(treeLayout);
        layout.add(new FinalDecoratorLayout("stateRewards"));
        layout.add(new FinalDecoratorLayout("ActionName"));
        layout.add(new RepaintAction());
       
        vis.putAction("color", color);
        vis.putAction("layout", layout);
    }
    
    /**
     * This function defines what data prefuse will be working with such as
     * how nodes connect to other nodes.
     * @throws ParseException 
     */
    public void setUpData(boolean init) throws ParseException
    {

        nodes = new ArrayList<>();
        edgeItems = new ArrayList<>();
        edges = new ArrayList<>();
        
        //set up graph and set all columns only once
        //if you set graph to new Tree visualization screws up
        //and then so does renderer and...you get the picture
        if(init)
        {
            graph = new Tree();
            graph.addColumn("state", String.class);
            graph.addColumn("reward", Double.class);
            graph.addColumn("nodeInfo", Integer.class);
            graph.addColumn("action", String.class);
            graph.addColumn("inPath", Integer.class);
            graph.addColumn("weight", float.class);
            graph.addColumn("type", String.class);
            graph.addColumn("stateClass", State.class);
            graph.addColumn("srcState", State.class);
            graph.addColumn("resultState", State.class);
            graph.addColumn("CriteriaAction", CriteriaAction.class);
            graph.addColumn("StateReward", Double.class);
            graph.addColumn("ActionName", String.class);
            graph.addColumn("CurrentState", boolean.class);
            generateComputeStates(tree.initialNode, null, true);
        }
        else
        {
            //we want to empty the graph if it has already been set up
            //but prefuse does not like it when these actions try to run on
            //an empty graph so we will stop them
            vis.cancel("color");
            vis.cancel("layout");
            vis.removeAction("color");
            vis.removeAction("layout");
            graph.clear();
        }
        
        visibleStates = new ArrayList();
        visibleActions = new ArrayList();
        
        Hashtable<String, CriteriaAction> definedActions = thisController.getActionMap();
        
        //largestAllowedReward is only used if degredation is set
        double optimalReward = thisController.getRewardWithCA(tree.actionsTaken);
        double largestAllowedReward = optimalReward * (1 +(degredation/100));
        
        //The below loop takes every computeable state and puts them in arrays
        for(int i = 0; i < computableStates.size(); i++)
        {
            List<State> onePath = new ArrayList();
            List<CriteriaAction> oneActionPath = new ArrayList();
            for(int j = 0; j < computableStates.get(i).prevStates.size(); j++)
            {
                onePath.add(computableStates.get(i).prevStates.get(j).s);
            }
            onePath.add(computableStates.get(i).thisState.s);
            
            if(computableStates.get(i).validEa)
            {
                for(int j = 0; j < computableStates.get(i).ea.stateSequence.size(); j++)
                {
                    onePath.add(computableStates.get(i).ea.stateSequence.get(j));
                }
            }
            for(int j = 0; j < computableStates.get(i).prevActions.size(); j++)
            {
                oneActionPath.add(computableStates.get(i).prevActions.get(j));
            }
            if(computableStates.get(i).validEa)
            {
                for(int j = 0; j < computableStates.get(i).ea.actionSequence.size();j++)
                {
                    String actName = computableStates.get(i).ea.actionSequence.get(j).actionName();
                    CriteriaAction act = definedActions.get(actName);
                    oneActionPath.add(act);
                }
            }

            
            
            //will soon implement the compute states to not include anything above degredataion 
            //which will remove the need for this if
            if(degredation >= 0)
            {
                //below are print statements that allow the programmer to see which paths were included
                double thisReward = thisController.getRewardWithCA(oneActionPath);
                boolean containRemovedConnections = false;
                
                if(thisReward >= largestAllowedReward && !containRemovedConnections)
                {
                    visibleStates.add(onePath);
                    visibleActions.add(oneActionPath);
                }
            }
            else
            {
                visibleStates.add(onePath);
                visibleActions.add(oneActionPath);
            }
        }

        
        
        setUpNodes(init);
        setPrefuseNodeConnections();
        setDefaultEdgeData();
        handleSubPathToTarget();
        handlePathToTarget();
        if(!init)
        {
            setStatePallette(false);
            setEdgePallette(false);
        }
    }
    
    //end public functions.
    //------------------------------------------------------------------------------------------------------
    //begin of private functions
    
    /**
     * sets the items for edgeItems and gives every node a black stroke
     */
    private void setItems()
    {
        //edges is only the edges from initial state to target state
        for(int i = 0; i < edges.size(); i++)
        {
            VisualItem item = vis.getVisualItem("graph.edges", edges.get(i));
            CriteriaAction thisAction = (CriteriaAction) item.get("CriteriaAction");
            item.set("reward", thisController.getRewardWithCA(thisAction));
            edgeItems.add(item);//this is used by dataDisplay later when we make buttons(When user clicks the button we want it to be 
                                //as if the user actually clicked that edge).
        }
        //give every node black stroke
        Iterator allNodes = graph.nodes();
        while(allNodes.hasNext())
        {
            Node tempNode = (Node) allNodes.next();
            VisualItem item = vis.getVisualItem("graph.nodes", tempNode);
            item.setStrokeColor(ColorLib.rgb(0, 0, 0));
            item.setStroke(new BasicStroke(0));
            if(item.getBoolean("CurrentState")) vis.getVisualItem("graph.nodes", tempNode).setStroke(new BasicStroke(10));
        }
        
        
//        for(int i = 0; i < graph.getEdgeCount(); i++)
//        {
//            vis.getVisualItem("graph.edges", graph.getEdge(i)).setSize(5);
//            vis.getVisualItem("graph.nodes", graph.getNode(i)).setSize(500);
//        }
        
    }
    /**
     * create nodes for each state
     */
    private void setUpNodes(boolean init)
    {        
        if(init)    graph.addRoot().set("CurrentState", true); //this adds the root and sets a needed column
        else      graph.addRoot();
        
        Node n = graph.getRoot();
        
        //optimal path loop
        for(int i = 0; i < tree.statesTaken.size(); i++)
        {
            n.set("type", "node");
            n.set("state", tree.statesTaken.get(i).getCompleteStateDescription()); 
            n.set("stateClass", tree.statesTaken.get(i));       
            double stateValueFunction = thisController.getV(tree.statesTaken.get(i));
            double finalStateValueFunction = (double) Math.round(stateValueFunction * 100000) / 100000; //the math.round goes to 5 places(up to the decimal point)
            n.set("StateReward", finalStateValueFunction);
            nodes.add(n);
            Node temp = n;
                
            //indexing required this type of statement since I wanted the last node to NOT have an edge
            //hence it being a "final" state
            if(i < tree.statesTaken.size() - 1)
            {
                n = graph.addChild(n);
                Edge edge = graph.getEdge(temp, n);
                edge.set("srcState", tree.statesTaken.get(i));//set the edges source state
                edge.set("resultState", tree.statesTaken.get(i + 1));//and target state
                edge.set("action", tree.actionsTaken.get(i).getName());
                edge.set("CriteriaAction", tree.actionsTaken.get(i));
                edge.set("ActionName", tree.actionsTaken.get(i).getName());
                edge.set("reward", thisController.getRewardWithCA(tree.actionsTaken.get(i)));
            }
        }
        //end optimal path loop
        
        
        //paths other than the otimal one
        //visibleStates.get(i) represents a list of states that represent one path
        //visibleAction.get(i) represents a list of actions that represent one path
        //both lists are parrell meaning the to go from state 1 to 2 action 1 was applied and so forth
        for(int i = 0; i < visibleStates.size(); i++)
        {
            n = graph.getRoot();
            State prevState = null;
            int numOfNodes = visibleStates.get(i).size();
            for(int j = 0; j < numOfNodes;j++)
            { 
                //this prevents "hidden" paths from being produced
                //the break stops the production of the path and moves to the next path
                if(temporaryHiddenStates.contains(visibleStates.get(i).get(j)))
                {
                    break;
                }
                
                
                boolean flag = false; //flag is whether a state in visible states already has a node dedicated
                                      //to representing that state.  This helps to make edges point to that
                                      //node rather than creating another node for the same state
                Iterator nodeIterator = graph.nodes();
                while(nodeIterator.hasNext())
                {
                    Node test = (Node) nodeIterator.next();
                    if(test.get("stateClass").equals(visibleStates.get(i).get(j))) //if its found set flag to true
                    {
                        flag = true; //copy was found
                    }
                    if(flag)//handle the copy
                    {
                        Edge e = graph.getEdge(n, test); //if e is not null it means this case has been handled before
                                                         //although this case seems rare it happens sometimes
                        if(e == null && prevState != null)
                        {
                            boolean add = true; //this flag is used to determine whether the state can be added based
                                                //on if that edge has been hidden by the user
                            
                            //the below loop just checks to make sure we are not adding an edge that the user has hidden
                            for(int a = 0; a < temporaryHiddentStateNodes.size(); a++)
                            {
                                if(temporaryHiddentStateNodes.get(a).s.equals(prevState) 
                                        && temporaryHiddentStateNodes.get(a).connections.get(0).action.equals(visibleActions.get(i).get(j-1)))
                                {
                                    add = false;
                                }
                            }
                            if(add)
                            {
                                e = graph.addEdge(n, test);
                                e.set("srcState", prevState);//set the edges source state
                                e.set("resultState", visibleStates.get(i).get(j));//and target state
                                e.set("action", visibleActions.get(i).get(j-1).getName());
                                e.set("CriteriaAction", visibleActions.get(i).get(j-1));
                                e.set("ActionName", visibleActions.get(i).get(j-1).getName());
                                e.set("reward", thisController.getRewardWithCA(visibleActions.get(i).get(j-1)));
                            }
                        }
                        flag = false;
                        j++;  //since we handled a state go ahead and increment j
                              //I would have liked to have used continue but since
                              //we are looping over the graph and not visibleStates
                              //continue would not have the desired effect
                              //the following if statement handles cases where j goes over 
                              //visibleState.size()
                        if(j >= visibleStates.get(i).size())//if we go over break
                        {
                            break;
                        }

                        prevState = visibleStates.get(i).get(j - 1);
                        n = test;
                        nodeIterator = graph.nodes(); //start the iteration again to check the WHOLE graph for the next state
                    }
                }
                //another checker in case j gets incremented to much
                if(j >= visibleStates.get(i).size())
                        break;
                
                //if this state was hidden stop here and just go to the next j
                if(temporaryHiddenStates.contains(visibleStates.get(i).get(j)))
                {
                    //idk whether break or continue is best will come back later
                    break;
//                    continue;
                }
                
                


                    boolean add = true; //this checks for indiv edges(such as that between a non-optimal node
                                        //to an optimal one where if hide branch was used it would have no node
                                        //to hide but just a single edge.  This is the fix for that
                    for(int a = 0; a < temporaryHiddentStateNodes.size(); a++)
                    {
                        if(temporaryHiddentStateNodes.get(a).s.equals(prevState) 
                            && temporaryHiddentStateNodes.get(a).connections.get(0).action.equals(visibleActions.get(i).get(j-1)))
                        {
                            add = false;
                        }
                                
                    }
                
                    
                Node temp = n; //temp keeps up with n's last node value
                if(j == 0 || add) //if this is the first or is supposed to be added then do add
                {
                    n = graph.addChild(n);
                }
                else
                {
                    break;
                }
                
                if(j < visibleStates.get(i).size())
                {
                    Edge edge = graph.getEdge(temp, n);
                    edge.set("srcState", prevState);//set the edges source state
                    edge.set("resultState", visibleStates.get(i).get(j));//and target state
                    edge.set("action", visibleActions.get(i).get(j-1).getName());
                    edge.set("CriteriaAction", visibleActions.get(i).get(j-1));
                    edge.set("ActionName", visibleActions.get(i).get(j-1).getName());
                    edge.set("reward", thisController.getRewardWithCA(visibleActions.get(i).get(j-1)));
                }
                
                n.set("type", "node");
                n.set("state", visibleStates.get(i).get(j).getCompleteStateDescription()); 
                n.set("stateClass", visibleStates.get(i).get(j));  
                prevState = visibleStates.get(i).get(j);
                double stateValueFunction = thisController.getV(visibleStates.get(i).get(j));
                double finalStateValueFunction = (double) Math.round(stateValueFunction * 100000) / 100000; //the math.round goes to 5 places(up to the decimal point)
                n.set("StateReward", finalStateValueFunction);
                
                TerminalFunction tf = new Termination();

                if(tf.isTerminal(visibleStates.get(i).get(j)))
                {
                    n.set("nodeInfo", 0); //blue node
                }
                else if(visibleStates.get(i).get(j).equals(tree.initialState))
                {
                    n.set("nodeInfo", 1); //red node
                }
                else
                {
                    n.set("nodeInfo", 3); //pink node
                }
            }
        } 
    }
    /**
     * exactly what the function names says. It creates the connections between nodes in the graph.
     */
    private void setPrefuseNodeConnections()
    {
        int counter = 0;
        
        for(int i = 0; i < tree.actionsTaken.size(); i++)
        {
            Node n1 = graph.getNode(counter);
            Node n2 = graph.getNode(counter + 1);
          
            State currentState = tree.statesTaken.get(i);
            State nextState = tree.statesTaken.get(i + 1);
            
                TerminalFunction tf = new Termination();
                if(tf.isTerminal(nextState))
                {
                    n2.set("nodeInfo", 0); //blue node
                }
                if(currentState.equals(tree.initialState))
                {
                    n1.set("nodeInfo", 1); //red node
                }
                else
                {
                    n1.set("nodeInfo", 3); //pink node
                }
            counter++;
        }
    }
    
    /**
     * This function just sets the default data for every edge.
     * <p>
     * For edges that are actions to the target state the weight and inPath parameters will change.
     */
    private void setDefaultEdgeData()
    {
        for(int i = 0; i < graph.getEdgeCount(); i++)
        {
            Edge e = graph.getEdge(i);
            e.set("type", "edge");
            e.set("weight", 1.0);
            e.set("inPath", 1);
        }
    }
    
    
    
    private void handleSubPathToTarget()
    {
        ComputeState cs = null;
        for(int i = 0; i < computableStates.size(); i++)
        {
            if(computableStates.get(i).validEa)
            {
                cs = computableStates.get(i);
                break;
            }
        }
        if(cs == null) return; //no suboptimal path to show
        else if(tree.statesTaken.contains(cs.thisState.s)) return; //if we are in a purple node the optimal path can be taken
        List<State> subOptimalPath = cs.convertToStateList();
        
        int stateCounter = 0;
        int stop = subOptimalPath.size() - 2; //took off 1 for indexing and another 1 to stop edge at state before the final state
        for(int i = 0; i < graph.getEdgeCount(); i++)
        {
            Edge e = graph.getEdge(i);
            if(e.getSourceNode().get("stateClass").equals(subOptimalPath.get(stateCounter))&&
                    e.getTargetNode().get("stateClass").equals(subOptimalPath.get(stateCounter + 1)))
            {
                e.set("inPath", 2);
                i = 0;
                stateCounter++;
                if(stateCounter > stop) break;
            }
        }
    }
    
    /**
     * This function sets data needed for the nodes and edges from the initial state to the target state
     */
    private void handlePathToTarget()
    {
        for(int i = 0; i < tree.stateNodesTaken.size() - 1; i++)
        {
            //the following lines work because the indexes inside the graph
            //are the same as the indexes in the tree
            
            //The reason this is is because I added the optimal path first
            StateNode src = tree.stateNodesTaken.get(i);
            StateNode target = tree.stateNodesTaken.get(i+1);
            int indexSrc = i;
            int indexTarget = i+1;
            Node nodeSrc = graph.getNode(indexSrc);
            Node nodeTarget = graph.getNode(indexTarget);
            
            stateValueContainer.addStateValue(thisController.getV(src.s));
            actionValueContainer.addAction(src.s, target.s, src.checkIfResultState(target.s));
            
            if(!target.isTerminal()) nodeTarget.set("nodeInfo", 2); //purple nodes
            if(!nodeSrc.equals(nodeTarget)) //if the result state is the same as the src then nothing needs to happen
            {
                Edge e = graph.getEdge(nodeSrc, nodeTarget); //get the edge between the nodes
            
                if(e != null) //and change it accordingly
                {
                e.set("inPath", 0);  
                e.set("weight", 1.0);
                edges.add(e); //edges is a list of the edges from initial to target
                }
            }
                        
        }//end of loop
        stateValueContainer.addStateValue(0); //final state has 0 value
    }
    
    //below function was an early attempt at looking at state tree
    private void generateComputeStates(ComputeState prevState)
    {
        for(int i = 0; i < prevState.thisState.connections.size(); i++)
        {
            for(int j = 0; j < prevState.thisState.connections.get(i).nodes.size(); j++)
            {
                StateNode nextNode = findInTree(prevState.thisState.connections.get(i).nodes.get(0).s);
                ComputeState nextState = new ComputeState();
                for(int k = 0; k < prevState.prevStates.size(); k++)
                {
                    nextState.prevStates.add(prevState.prevStates.get(k));
                }
                for(int k = 0; k < prevState.prevActions.size(); k++)
                {
                    nextState.prevActions.add(prevState.prevActions.get(k));
                }
                boolean shouldAdd = true;
                nextState.thisState = nextNode;
                for(int k = 0; k < computableStates.size(); k++)
                {
                    if(computableStates.get(k).thisState.equals(nextState.thisState))
                    {
                        shouldAdd = false;
                    }
                }
                nextState.prevStates.add(prevState.thisState);

                nextState.prevActions.add(prevState.thisState.connections.get(i).action);
                if(shouldAdd)
                {
                    this.computableStates.add(nextState);
                }
                nextState.ea = thisController.getOptimalPathFrom(nextState.thisState.s);
                generateComputeStates(nextState);
            }
        }
        
    }
    
    
    /**
     * This is the method that handles adding states by the expand feature
     * @param initState
     * @param prevAction
     * @param prevState 
     */
    public void addComputeState(StateNode initState, CriteriaAction prevAction, State prevState, boolean isExpanding)
    {
        int visibleStateNum = 0; //index in visibleState that the prevState and initState.s are found
        int endIndex = 0; //index of initState in the sublist at [visibleStateNum]
        
        for(int i = 0; i < initState.connections.size(); i++)
        {
            for(int j = 0; j < initState.connections.get(i).nodes.size(); j++)
            {
//                if(dontComputeStates.contains(initState.connections.get(i).states.get(j))) continue;
                boolean found = false;
                ComputeState newCompute;
                ComputeState oldCompute = findInAlreadyComputedStates(initState.connections.get(i).nodes.get(j));
                if(oldCompute != null)
                {
                    found = true;
                }
                

                    newCompute = new ComputeState();
                    
                    for(int a = 0; a < chosenStates.size(); a++)
                    {
                        newCompute.prevStates.add(tree.getNodeForState(chosenStates.get(a)));
                    }
                    for(int a = 0; a < chosenActions.size(); a++)
                    {

                        newCompute.prevActions.add(chosenActions.get(a));
                    }
                    
                    
                if(isExpanding)
                {
                    State nextState = newCompute.prevStates.get(newCompute.prevStates.size() - 1).s;
                    newCompute.prevStates.remove(newCompute.prevStates.size() - 1);
                    FinalControlListener.ContainerOfActionAndStateSeqence cont = mouse.getPathFrom(nextState, initState.s);
                    if(cont != null)
                    {
                        for(int a = 0; a < cont.states.size(); a++)
                        {
                            for(int b = 0; b < tree.nodes.size(); b++)
                            {
                                if(tree.nodes.get(b).s.equals(cont.states.get(a)))
                                {
                                    newCompute.prevStates.add(tree.nodes.get(b));
                                    break;
                                }
                            }
                        }
                        for(int a = 0; a < cont.actions.size(); a++)
                        {
                            newCompute.prevActions.add(cont.actions.get(a));
                        }
                    }
                    
                    else
                    {
                        newCompute.prevStates.add(lastComputeState.thisState);
                    }
                    newCompute.prevActions.add(initState.connections.get(i).action);
                }

                else
                {                             
                    newCompute.prevActions.add(initState.connections.get(i).action);
                }
                    //------------------------------------------
                    //end filling prevStates and prevActions
                
                
                                
                newCompute.thisState = initState.connections.get(i).nodes.get(j);
                
                if(found == false)
                {
                    newCompute.ea = thisController.getOptimalPathFrom(initState.connections.get(i).nodes.get(j).s);
                    newCompute.ea.stateSequence.remove(0); //the first stateSequence is the current state which is already included so get rid of it
                }
                else
                {
//                    newCompute.prevActions.clear();
                    mergeOldComputetoNewCompute(newCompute, oldCompute);
                }
                
                newCompute.validEa = false;


                //get reward of proposed path
                //I had to create a new list proposedPath so that I could group all
                //the actions up without messing up the integrity of each seperate list
                //in the compute state.  If only java provided a easy to use deep copy method.....sigh.....
                List<CriteriaAction> proposedPath = new ArrayList();
                for(int k = 0; k < newCompute.prevActions.size(); k++)
                {
                    proposedPath.add(newCompute.prevActions.get(k));
                }
                for(int k = 0; k < newCompute.ea.actionSequence.size(); k++)
                {
                    CriteriaAction plannedAct = thisController.getActionMap().get(newCompute.ea.actionSequence.get(k).actionName());
                    proposedPath.add(plannedAct);
                }
                
                double totalReward = thisController.getRewardWithCA(proposedPath);
                
                double optimalReward = thisController.getRewardWithCA(tree.actionsTaken);
                double largestAllowedReward = optimalReward * (1 +(degredation/100));
                
                boolean alreadyComputed = false;
                for(int a = 0; a < dontComputeStates.size(); a++)
                {
                    if(newCompute.convertToStateList().contains(dontComputeStates.get(a)))
                    {
                        alreadyComputed = true;
                    }
                }
                //remember if degredation is < 0 it is not set
                if((totalReward > largestAllowedReward && !alreadyComputed) || this.degredation < 0)
                {
                    
//                    if(!computableStates.contains(newCompute) || 
//                             && !checkForExistInTree(newCompute.thisState))
                    {
                        computableStates.add(newCompute);
                    }
                    if(!oldComputeStates.contains(newCompute))
                    {
                        oldComputeStates.add(newCompute);
                    }
                    
                }
                else
                {
                    dontComputeStates.add(newCompute.thisState.s);
                }
            }
        }
    }
    
    private void mergeOldComputetoNewCompute(ComputeState newCompute, ComputeState oldCompute)
    {
        if(newCompute.thisState.equals(oldCompute.thisState)) 
        {
            newCompute.ea = new EpisodeAnalysis();
            for(int i = 0; i < oldCompute.ea.actionSequence.size(); i++)
            {
                newCompute.ea.actionSequence.add(oldCompute.ea.actionSequence.get(i));
            }
            for(int i = 0; i < oldCompute.ea.stateSequence.size(); i++)
            {
                newCompute.ea.stateSequence.add(oldCompute.ea.stateSequence.get(i));
            }
        }
    }
    
    
    
/**
 * This is called after the current state moves.
 * @param initState
 * @param prevAction
 * @param changeCurrentState 
 */
    public void generateComputeStates(StateNode initState, CriteriaAction prevAction, boolean changeCurrentState) 
    {
        State prevState = null;
        for(int i = 0; i < oldComputeStates.size(); i++)
        {
            oldComputeStates.get(i).validEa = false;
        }
        if(changeCurrentState)
        {
            computableStates = new ArrayList<>();
        }
        
        
        //if null we set lastComputeState to initialState and ea to the very first ea
        if(lastComputeState == null) 
        {
            lastComputeState = new ComputeState();
            lastComputeState.thisState = initState;
            lastComputeState.ea = thisController.getEpisodeAnalysis();
            List<State> stateSeq = new ArrayList();
            List<GroundedAction> actionSeq = new ArrayList();
            for(int i = 1; i < lastComputeState.ea.stateSequence.size(); i++)
            {
                stateSeq.add(lastComputeState.ea.stateSequence.get(i));
            }
            for(int i = 0; i < lastComputeState.ea.actionSequence.size(); i++)
            {
                actionSeq.add(lastComputeState.ea.actionSequence.get(i));
            }
            lastComputeState.ea.stateSequence = stateSeq;
            lastComputeState.ea.actionSequence = actionSeq;
        }
        else if(changeCurrentState)
        {
            lastComputeState.prevStates.add(lastComputeState.thisState);
            lastComputeState.prevActions.add(prevAction);
            lastComputeState.thisState = initState;
            
            
            if(!lastComputeState.ea.stateSequence.isEmpty() && lastComputeState.thisState.s.equals(lastComputeState.ea.stateSequence.get(0)))
            {
                lastComputeState.ea.stateSequence.remove(0);
                lastComputeState.ea.actionSequence.remove(0);
            }
            
            else if(tree.stateNodesTaken.contains(initState))
            {
                lastComputeState.ea = new EpisodeAnalysis(); //the optimal path is always shown so no need for
                                                             //a real EpisodeAnaylsis
            }
            
            else
            {
                ComputeState temp = findInAlreadyComputedStates(initState);
                mergeOldComputetoNewCompute(lastComputeState, temp);
                //keep the below comments. still testing
//                if(temp != null)
//                {
//                    mergeOldComputetoNewCompute(lastComputeState, temp);
//                }
//                else
//                {
////                    System.out.println("recomputing!!!");
//                    lastComputeState.ea = thisController.getOptimalPathFrom(initState.s);//this line must change
//                    lastComputeState.ea.stateSequence.remove(0);
//                }
            }
        }
        
        lastComputeState.validEa = true; //the ea from the current state should always be visible(sub optimal path)
        
        if(lastComputeState.prevStates.size() > 1)
        {
            prevState = lastComputeState.prevStates.get(lastComputeState.prevStates.size() - 1).s;
        }
        
        if(!lastComputeState.ea.stateSequence.isEmpty() && lastComputeState.thisState.s.equals(lastComputeState.ea.stateSequence.get(0)))
        {
            lastComputeState.ea.actionSequence.remove(0);
            lastComputeState.ea.stateSequence.remove(0);
        }
        addComputeState(initState, prevAction, prevState, false);
        computableStates.add(lastComputeState);
        lastComputeState.validEa = true;

    }
    
    
    /**
     * if a state is found and already has a computed ea find it here and use that 
     * sequence rather than recomputing
     * @param s
     * @return 
     */
    private ComputeState findInAlreadyComputedStates(StateNode s)
    {
        boolean inEA = false;
        int index = 0;
        int subIndex = 0;
        
        for(int i = 0; i < oldComputeStates.size(); i++)
        {
            
            ComputeState testState = oldComputeStates.get(i);
            if(testState.thisState.equals(s))
            {
                return testState;
            }
            
            else if(testState.ea.stateSequence.contains(s.s))
            {
                inEA = true;
                index = i;
                subIndex = testState.ea.stateSequence.indexOf(s.s) + 1;
                break;
            }
        }
        ComputeState newCompute = new ComputeState();
        
        if(inEA)
        {
            //fill prev states
            for(int i = 0; i < chosenStates.size() - 1; i++)
            {
                newCompute.prevStates.add(tree.getNodeForState(chosenStates.get(i)));
            }
            newCompute.thisState = tree.getNodeForState(chosenStates.get(chosenStates.size() - 1)); //last element of chosenStates is currentState
            
            //fill prevActions
            for(int i = 0; i < chosenActions.size(); i++)
            {
                newCompute.prevActions.add(chosenActions.get(i));
            }
            
            newCompute.ea = new EpisodeAnalysis();
            
            //fill the ea with the proper info
            for(int i = subIndex; i < oldComputeStates.get(index).ea.stateSequence.size(); i++)
            {
                newCompute.ea.stateSequence.add(oldComputeStates.get(index).ea.stateSequence.get(i));
            }
            
            for(int i = subIndex; i < oldComputeStates.get(index).ea.actionSequence.size(); i++)
            {
                newCompute.ea.actionSequence.add((GroundedAction) oldComputeStates.get(index).ea.actionSequence.get(i));
            }
             


            //no need to compute reward since all computed states that are added
            //already meet the reward criteria
            return newCompute;
        }
        
        
        return null;
    }
    
    
    private double getRewardForComputeState(ComputeState compute)
    {
        List<CriteriaAction> actions = new ArrayList();
        for(int i = 0; i < compute.prevActions.size(); i++)
        {
            actions.add(compute.prevActions.get(i));
        }
        for(int i = 0; i < compute.ea.actionSequence.size(); i++)
        {
            CriteriaAction act = thisController.getActionMap().get(compute.ea.actionSequence.get(i).actionName());
            actions.add(act);
        }
        return thisController.getRewardWithCA(actions);
    }

    private StateNode findInTree(State s)
    {
        for(int i = 0; i < tree.nodes.size(); i++)
        {
            if(tree.nodes.get(i).s.equals(s)) return tree.nodes.get(i);
        }
        return null;
    }

    void rehighlightPath(boolean changeCurrentState, boolean removeHiddenBan) 
    {
        
        temporaryHiddenStates = new ArrayList();
        temporaryHiddentStateNodes = new ArrayList<StateNode>();
        
        for(int i = 0; i < graph.getNodeCount(); i++)
        {
            if(!graph.getNode(i).get("stateClass").equals(lastComputeState.thisState.s) && graph.getNode(i).getBoolean("CurrentState") && changeCurrentState)
            {
                graph.getNode(i).set("CurrentState", false);
                vis.getVisualItem("graph.nodes", graph.getNode(i)).setStroke(new BasicStroke(0));
            }
            if(graph.getNode(i).get("stateClass").equals(lastComputeState.thisState.s))
            {
                graph.getNode(i).set("CurrentState", true);
                vis.getVisualItem("graph.nodes", graph.getNode(i)).setStroke(new BasicStroke(10));
            }
        }
        
        //for each chosen state
        for(int i = 0; i < this.chosenStates.size() - 1; i++)
        {
            //find in the graph
            for(int j = 0; j < graph.getNodeCount(); j++)
            {
                //ok we found the state in the graph
                if(graph.getNode(j).get("stateClass").equals(this.chosenStates.get(i)))
                {
                    //now find the NEXT state in the graph
                    for(int k = 0; k < graph.getNodeCount(); k++)
                    {
                        //now we have found both states in the graph
                        if(graph.getNode(k).get("stateClass").equals(this.chosenStates.get(i + 1)))
                        {
                            //get the edge and set the weight accordingly
                            graph.getEdge(graph.getNode(j), graph.getNode(k)).set("weight", 200);
                        }
                    }
                }
            }
        }
    }
    
    private void setStatePallette(boolean init)
    {
        boolean pinkNode = false;
        for(int i = 0; i < graph.getNodeCount(); i++)
        {
            if(graph.getNode(i).get("nodeInfo").equals(3))
            {
                pinkNode = true;
                break;
            }
        }
        if(pinkNode) 
        {
            s_pallette = Originals_pallette.clone();
        }
        else
        {
            s_pallette = new int[3];
            s_pallette[0] = Originals_pallette[0];
            s_pallette[1] = Originals_pallette[1];
            s_pallette[2] = Originals_pallette[2];
        }
        if(!init)
        {
            color.remove(fill);
            fill = new DataColorAction("graph.nodes", "nodeInfo", Constants.ORDINAL, VisualItem.FILLCOLOR, s_pallette);   
            fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,255, 0));
            color.add(fill);
        }

    }
    
    /**
     * This function changes the color pallete for edges based on what type of actions are present.
     * @param init 
     */
    private void setEdgePallette(boolean init)
    {
        boolean subOptimalPath = false;
        boolean justAPath = false;
        for(int i = 0; i < graph.getEdgeCount(); i++)
        {
            if(graph.getEdge(i).get("inPath").equals(2))
            {
                subOptimalPath = true;
            }
            if(graph.getEdge(i).get("inPath").equals(1))
            {
                justAPath = true;
            }
        }
        if(subOptimalPath && justAPath)
        {
            e_pallette = new int[3];
            e_pallette[0] = Originale_pallette[0];
            e_pallette[1] = Originale_pallette[1];
            e_pallette[2] = ColorLib.rgb(0, 191, 255);
        }
        else if(subOptimalPath)
        {
            e_pallette = new int[2];
            e_pallette[0] = Originale_pallette[0];
            e_pallette[1] = ColorLib.rgb(0, 191, 255);
        }
        else if(justAPath)
        {
            e_pallette = Originale_pallette.clone();
        }
        else
        {
             e_pallette = new int[1];
            e_pallette[0] = Originale_pallette[0];
        }
        if(!init)
        {
            color.remove(dcaEdges);
            color.remove(arrowColor);
            dcaEdges = new DataColorAction("graph.edges", "inPath",  Constants.NOMINAL, VisualItem.STROKECOLOR, e_pallette);
            dcaEdges.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 255, 0));
            arrowColor = new DataColorAction("graph.edges", "inPath",Constants.NOMINAL, VisualItem.FILLCOLOR, e_pallette);
            arrowColor.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 255, 0));
            color.add(dcaEdges);
            color.add(arrowColor);
        }
    }
}


