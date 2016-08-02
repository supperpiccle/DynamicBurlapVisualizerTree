/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dynamicmdpcontroller;

import burlap.behavior.singleagent.Episode;

/**
 *
 * @author Justin Lewis
 */
public class EpisodeContainer 
{
    public Episode e;
    public DynamicMDPState s;
    
    public EpisodeContainer()
    {
        
    }
    
    public void createEpisode(DynamicMDPState s)
    {
        this.s = s;
        e = new Episode(s);
    }
}
