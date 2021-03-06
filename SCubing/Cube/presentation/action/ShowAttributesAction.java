package presentation.action;

import gov.nasa.worldwind.layers.Layer;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import org.geotools.data.simple.SimpleFeatureSource;

import presentation.layers.SimpleFeatureLayer;
import presentation.layout.ApplicationTemplate;
import presentation.layout.MapFrame;
import bll.util.WorldWindUtils;


public class ShowAttributesAction extends AbstractAction
{
	ApplicationTemplate.AppFrame appFrame;
	private Layer layer;
	JPanel shapeOptionsPane;


	public ShowAttributesAction(Layer layer, ApplicationTemplate.AppFrame appFrame, JPanel shapeOptionsPane)
	{
		//super(layer.getName());
		this.appFrame = appFrame;
		this.layer = layer;
		this.shapeOptionsPane = shapeOptionsPane;
	}




	public void actionPerformed(ActionEvent arg0) {


		if(layer instanceof SimpleFeatureLayer)
		{

			//TODO: Tirar isso daqui	
			MapFrame.getInstance().setSelectedLayer((SimpleFeatureLayer) layer);
			WorldWindUtils.flyTo(((SimpleFeatureLayer)layer).getSector() , appFrame.getWwd() );
			
			//selected = true;
			appFrame.getBottomPanel().setSource((SimpleFeatureSource) ((SimpleFeatureLayer)layer).getFeatureSource());
			try {
				//appFrame.getBottomPanel().setSize(0,0);
				appFrame.getBottomPanel().filterFeatures();

				enableTree(shapeOptionsPane, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//
		}
		else
		{
			//appFrame.getBottomPanel().setSize(0,50);
			enableTree(shapeOptionsPane, false);
		}


	}

	public void enableTree(Container root, boolean enable) {

		Component children[] = root.getComponents();
		for(int i = 0; i < children.length; i++) {

			if(children[i] instanceof JPanel) {

				enableTree((Container)children[i], enable);
			} else {

				children[i].setEnabled(enable);
			}
		}
	}

}