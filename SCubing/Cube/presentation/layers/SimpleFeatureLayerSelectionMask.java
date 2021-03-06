/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package presentation.layers;

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceShape;
import gw.events.FeatureSelectionListener;

import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;

import bll.util.WorldWindUtils;

/**
 *
 * Handles selections and tooltips for SimpleFeatureLayers
 * (note that the SimpleFeatureLayer creates this helper class automatically,
 * so you shouldn't need to instantiate this directly)
 *
 */
public class SimpleFeatureLayerSelectionMask implements SelectListener{

	SimpleFeatureLayer baseShapeLayer;
	GlobeAnnotation tooltip;
	SimpleFeature tooltipFeature;
	ArrayList<SurfaceShape> selections = new ArrayList<SurfaceShape>();
	List<FeatureSelectionListener> featureSelectionListeners = new ArrayList<FeatureSelectionListener>();



	/**
	 *
	 * @param sl
	 */
	public SimpleFeatureLayerSelectionMask(SimpleFeatureLayer sl) {
		baseShapeLayer = sl;
	}



	/**
	 * Called whenever the base shapefile layer is clicked.
	 *
	 * @param event
	 */
	
	public void selected(SelectEvent event) {

		if (event.getMouseEvent() == null) {
			return;
		}

		if (event.getEventAction().equals(SelectEvent.LEFT_CLICK) || event.getEventAction().equals(SelectEvent.RIGHT_CLICK)) {
			PickedObject obj = event.getTopPickedObject();
			if (obj != null && (obj.getObject() instanceof SurfaceShape)) {
				try {
					LatLon latLon = new LatLon(baseShapeLayer.getMousePosition().latitude,baseShapeLayer.getMousePosition().longitude);

					if (obj.getObject() instanceof SurfaceShape)
					{
						if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
							selectedFeature(((SurfaceShape)obj.getObject()).getValue("ID").toString(),((SurfaceShape)obj.getObject()));
						} else if (event.getEventAction().equals(SelectEvent.RIGHT_CLICK)) {
							SimpleFeature featureAtMousePoint = baseShapeLayer.getFeatureAt(latLon);
							showTooltip(featureAtMousePoint);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Removes any currently displayed tooltips.
	 */
	public void clearTooltip() {
		if (tooltip != null) {
			this.baseShapeLayer.removeRenderable(tooltip);
		}
	}

	/**
	 * Creates a renderable tooltip for a given feature.
	 *
	 * @param feature
	 */
	public void showTooltip(SimpleFeature feature) {
		clearTooltip();
		if (feature.equals(tooltipFeature)) {
			tooltipFeature = null;
			return;
		}
		tooltipFeature = feature;
		String txt = "";
		for (int i = 1; i < feature.getAttributeCount(); i++) {
			txt = txt + feature.getFeatureType().getType(i).getName().getLocalPart() +
			": " + feature.getAttribute(i).toString() + "\n";
		}
		LatLon latLon = new LatLon(baseShapeLayer.getMousePosition().latitude,baseShapeLayer.getMousePosition().longitude);
		tooltip = new GlobeAnnotation(txt, new Position(latLon, 0));
		tooltip.getAttributes().setInsets(new Insets(1, 1, 1, 1));
		tooltip.getAttributes().setBorderColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
		tooltip.getAttributes().setBackgroundColor(new Color(1.0f, 1.0f, 0.85f, 1.0f));
		tooltip.getAttributes().setTextColor(Color.BLACK);
		tooltip.getAttributes().setScale(0.8);
		tooltip.setAlwaysOnTop(true);
		baseShapeLayer.addRenderable(tooltip);
	}



	/** 
	 * Add a feature selection Listener.
	 * @param l the listener to add
	 */
	public void addFeatureSelectionListener(FeatureSelectionListener l) {
		featureSelectionListeners.add(l);
	}

	/** 
	 * Remove a feature selection Listener
	 * @param l the listener to remove
	 */
	public void removeFeatureSelectionListener(FeatureSelectionListener l) {
		featureSelectionListeners.remove(l);
	}




	public SurfaceShape getFeatureSelected(SimpleFeature feature) {
		for (SurfaceShape s : selections) {
			if (s.getValue("ID").equals(feature.getID())){
				return s;
			}
		}
		return null;
	}




	/**
	 * Sets a feature as being selected, without clearing any other selections.
	 * Note that this can be canceled by any listeners if they do not confirm
	 * the selection.
	 *
	 * @param feature The feature to set as selected.
	 * @throws java.lang.Exception
	 */
	public void displaySelectedFeatures(Set<String> IDs)
	{
		/*SurfaceShape aux= getFeatureSelected(simpleFeature);
		if (aux!=null){
			aux.setAttributes(WorldWindUtils.getUnselectedAtts());
			selections.remove(aux);
		}*/


		for (Renderable renderable : baseShapeLayer.getRenderables()) {
			if (renderable instanceof SurfaceShape)
			{
				if (IDs.contains(((SurfaceShape)renderable).getValue("ID").toString()))

				{
					((SurfaceShape)renderable).setAttributes(WorldWindUtils.getSelectedAtts());
					selections.add((SurfaceShape)renderable);
					

				}
				else
				{
					((SurfaceShape)renderable).setAttributes(WorldWindUtils.getUnselectedAtts());
					selections.remove((SurfaceShape)renderable);
				}
			}
		}
	}




	public void selectedFeature(String featureID,SurfaceShape renderableShape) {

		if (selections.contains(renderableShape)) {
			selections.remove(renderableShape);
			renderableShape.setAttributes(WorldWindUtils.getUnselectedAtts());
			selectTableRegister(featureID,false);
		}
		else {
			selections.add(renderableShape);
			renderableShape.setAttributes(WorldWindUtils.getSelectedAtts());
			selectTableRegister(featureID,true);
		}
	}



	public void selectTableRegister(String featureID, boolean sel)
	{
		baseShapeLayer.canvasPane.getBottomPanel().selectTableRow(featureID,sel);
	}

	/**
	 * Clears all the current selections.
	 */
	public void clearSelection() {
		try {
			ShapeAttributes attr = WorldWindUtils.getUnselectedAtts();
			for (SurfaceShape s : selections) {
				s.setAttributes(attr);
			}
			this.selections.clear();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
