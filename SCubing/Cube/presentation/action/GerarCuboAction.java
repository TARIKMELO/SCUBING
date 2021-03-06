
/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package presentation.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.geotools.swing.MapPane;

import presentation.layout.CubeWindow;
import presentation.layout.MapFrame;

/**
 * An action to de-select any active map cursor tool.
 * 
 * @source $URL: http://svn.osgeo.org/geotools/tags/8.0-M2/modules/unsupported/swing/src/main/java/org/geotools/swing/action/NoToolAction.java $
 * @version $Id: NoToolAction.java 38094 2011-09-27 03:37:36Z mbedward $
 */
public class GerarCuboAction extends AbstractAction {
    /**
	 * 
	 */
	private static final long serialVersionUID = 4328043871858603673L;

	/** Name for this tool */
    public static final String TOOL_NAME = "Data Cube";
    
    /** Tool tip text */
    public static final String TOOL_TIP = "Create Data Cube...";
    
    /** Icon for the control */
    public static final String ICON_IMAGE = "/org/geotools/swing/icons/wp.png";
    /**
     * Constructor. The associated control will be labelled with an icon.
     * 
     * @param mapPane the map pane being serviced by this action
     */
    public GerarCuboAction(MapPane mapPane) {
        
    }

    
    
    /**
     * Called when the control is activated. Calls the map pane to reset the 
     * display 
     *
     * @param ev the event (not used)
     */
   
    //TODO: isolar isto aqui através de outra interface
    public void actionPerformed(ActionEvent ev) {
       
    	CubeWindow cubeWindow = new CubeWindow(MapFrame.getInstance().getSelectedLayerFeatureSource());
		cubeWindow.createSShell();
	
		cubeWindow.sShell.open();
		cubeWindow.sShell.forceActive();
		org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getDefault();
		while (!cubeWindow.sShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();   
    }      
}
