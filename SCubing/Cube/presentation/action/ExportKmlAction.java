package presentation.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xml.Encoder;

import presentation.layout.MapFrame;
import bll.util.Util;

public class ExportKmlAction extends AbstractAction{


	public ExportKmlAction() {

	}

	
	

	public void actionPerformed(ActionEvent ev) {

		try {
			collectionToKMLFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void collectionToKMLFile() throws IOException
	{

		if (MapFrame.getInstance()!=null && MapFrame.getInstance().getSelectedLayerFeatureSource()!=null)
		{
			
			
			
			
			KMLConfiguration kmlConfiguration =  new KMLConfiguration();
			File newFile =Util.getNewFile("kml");
			Encoder lEncoder = new Encoder(kmlConfiguration);
			FileOutputStream lFileOutputStream = new FileOutputStream(newFile);
			lEncoder.setIndenting(true);
			//lEncoder.encode(iPolygonsCollection, KML.kml, lFileOutputStream);
			lEncoder.encode( MapFrame.getInstance().getSelectedLayerFeatureSource().getFeatures(), KML.kml, lFileOutputStream);
		
			lEncoder.getDocument().getChildNodes();
		
			/*<Style id="examplePolyStyle">
		    <PolyStyle>
		      <color>ff0000cc</color>
		      <colorMode>random</colorMode>
		    </PolyStyle>
		  </Style>*/

			
			lEncoder.setIndenting(true);	
			lFileOutputStream.flush();
			lFileOutputStream.close();
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Favor selecione um layer vãlido para exportar!");
		}

	}
}