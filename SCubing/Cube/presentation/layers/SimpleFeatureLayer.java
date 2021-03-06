/*
 * $Id: AnimatedShapefileLayer.java 28 2008-10-05 16:29:18Z iovergard $
 * 
 * This file is a part of the GeoWind package, a library for visualizing
 * data from GeoTools in Nasa WorldWind.
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *     1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *     2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *     3. This notice may not be removed or altered from any source
 *     distribution.
 */
package presentation.layers;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceCircle;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.util.WWUtil;
import gw.events.FeatureSelectionListener;
import gw.util.ColorBlend;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JOptionPane;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import presentation.layout.ApplicationTemplate;
import bll.util.GeoToolsUtils;
import bll.util.Util;
import bll.util.WorldWindUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class SimpleFeatureLayer extends RenderableLayer implements PositionListener {

	CoordinateReferenceSystem originalCRS;
	FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
	MapContent map;
	Envelope initialBounds;
	ReferencedEnvelope layerBounds;
	Style style;


	double attrMin, attrMax;
	int attrNum = -1;
	Color attrMinColor = Color.BLACK;
	Color attrMaxColor = Color.WHITE;

	SimpleFeatureLayerSelectionMask selectionMask;
	//SimpleFeature[] features;
	boolean drawLines = true;
	long lastCursorMoveTime = 0;
	Position mousePosition = Position.ZERO;

	public ApplicationTemplate.AppFrame canvasPane;

	public SimpleFeatureLayer(String name, FeatureSource<SimpleFeatureType, SimpleFeature> source,
			Style s, ApplicationTemplate.AppFrame canvasPane) throws IOException, FactoryException, TransformException {

		this.canvasPane = canvasPane;

		boolean isEmpty = true;
		try
		{
			isEmpty = source.getFeatures().getBounds().isEmpty();
		}
		catch(Exception nullPointerEx)
		{
			isEmpty = true;
		}

		if (isEmpty)
		{
			createAlfaNumericLayer(name, source,	s,canvasPane.getWwd());
		}
		else 
		{
			createObjectsLayer(name, source,	s,canvasPane.getWwd());
		}
	}


	public void createAlfaNumericLayer(String name, FeatureSource<SimpleFeatureType, SimpleFeature> source,
			Style s, WorldWindow canvas) throws IOException
	{
		setName(name);
		featureSource = source;
		map = new MapContent();
		map.addLayer(new FeatureLayer( featureSource, style));
	}

	public void createObjectsLayer(String name, FeatureSource<SimpleFeatureType, SimpleFeature> source,	Style s, WorldWindow canvas) throws IOException
	{



		setName(name);
		featureSource = source;
		style = s;
		map = new  MapContent();
		map.addLayer(new FeatureLayer( featureSource, style));

		//originalCRS = map.getCoordinateReferenceSystem();
		originalCRS = featureSource.getSchema().getCoordinateReferenceSystem();

		if (originalCRS == null)
		{
			System.out.println("Não é possível gerar o visualização.\nO mapa não possui Sistemas de referência de coordenadas.");
			//JOptionPane.showMessageDialog(null, "Não ã possãvel gerar o visualizaãão.\nO mapa não possui Sistemas de referãncia de coordenadas.");
			return ;
		}

		//TODO:
		//map.setCoordinateReferenceSystem(ProjectionUtils.getDefaultCRS());
		//store the features locally
		//features = (SimpleFeature[]) featureSource.getFeatures().toArray();

		//project the coordinates
		//calculateProjectedCoordinates(getSector());
		layerBounds = map.getMaxBounds();

		//Create the main surface image
		Class<?> geomType = featureSource.getSchema().getGeometryDescriptor().getType().getBinding();
		if(geomType.isAssignableFrom(MultiPolygon.class)  )
		{

			for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
				SimpleFeature feature = (SimpleFeature) iter.next();

				//for (SimpleFeature feature : featureSource.getFeatures().features()) {
				MultiPolygon mp = (MultiPolygon) feature.getDefaultGeometry();
				int n = mp.getNumGeometries();
				for (int i = 0; i < n; i++) {
					Polygon poly = (Polygon) mp.getGeometryN(i);
					ArrayList<LatLon> surfaceLinePositions = new ArrayList<LatLon>();

					for (Coordinate latLon : (poly.getCoordinates())) {

						surfaceLinePositions.add(LatLon.fromDegrees(latLon.y, latLon.x));
					}  
					SurfaceShape shape = new SurfacePolygon(surfaceLinePositions);
					shape.setAttributes(WorldWindUtils.getUnselectedAtts());
					shape.setValue("ID", feature.getID());
					this.addRenderable(shape);
				}
			}
		}

		else if (geomType.isAssignableFrom(Polygon.class))
		{
			for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
				SimpleFeature feature = (SimpleFeature) iter.next();
				SurfaceShape polygon = WorldWindUtils.polygonFromFeature(feature, originalCRS);
				this.addRenderable(polygon);
			}
		}


		else if(geomType.isAssignableFrom(MultiLineString.class) )
		{

			for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
				SimpleFeature feature = (SimpleFeature) iter.next();
				Polyline polyLine = WorldWindUtils.polylineFromFeature(feature, originalCRS, 0);
				this.addRenderable(polyLine);
			}
		}
		else if(geomType.isAssignableFrom(LineString.class))
		{
			for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
				SimpleFeature feature = (SimpleFeature) iter.next();
					Polyline polyLine = WorldWindUtils.polylineFromFeature(feature, originalCRS, 0);
					this.addRenderable(polyLine);
				}
			} 
			else if (geomType.isAssignableFrom(Point.class))
			{
				for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
					SimpleFeature feature = (SimpleFeature) iter.next();
					this.addRenderable(WorldWindUtils.pointFromFeature(feature, originalCRS));
				}
			}

			else if (geomType.isAssignableFrom(MultiPoint.class))
			{
				for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
					SimpleFeature feature = (SimpleFeature) iter.next();
					this.addRenderables(WorldWindUtils.multiPointFromFeature(feature, originalCRS));
				}
			}

			else
			{
				// Surface square over the center of the United states.
				ShapeAttributes attrs = new BasicShapeAttributes();
				// Surface circle over the center of the United states.

				attrs.setInteriorMaterial(Material.GREEN);
				attrs.setOutlineMaterial(new Material(WWUtil.makeColorBrighter(Color.GREEN)));
				attrs.setInteriorOpacity(0.5);
				attrs.setOutlineOpacity(0.8);
				attrs.setOutlineWidth(1);
				double  circleRadius = Double.parseDouble(Util.getConfig().getCircleRadius());
				for( FeatureIterator<SimpleFeature> iter=featureSource.getFeatures().features(); iter.hasNext(); ){
					SimpleFeature feature = (SimpleFeature) iter.next();
					Coordinate latLon = ((Geometry)feature.getDefaultGeometry()).getCoordinate();
					SurfaceShape shape = new SurfaceCircle(LatLon.fromDegrees(latLon.y, latLon.x),circleRadius);
					shape.setAttributes(attrs);
					shape.setValue("ID", feature.getID());

					this.addRenderable(shape);
				}
			}
			selectionMask = new SimpleFeatureLayerSelectionMask(this);
			canvas.addPositionListener(this);
			canvas.addSelectListener(selectionMask);
		}



		/**
		 * 
		 * @param f
		 */
		public void displaySelectedFeatures(Set<String> set) {
			selectionMask.displaySelectedFeatures(set);
		}



		/**
		 * 
		 * @param l the featureselectionListener to add.
		 */
		public void addFeatureSelectionListener(FeatureSelectionListener l) {
			selectionMask.addFeatureSelectionListener(l);
		}



		/**
		 * Clear all selections.
		 */
		public void clearSelections() {
			selectionMask.clearSelection();
		}

		/**
		 * 
		 * @param feature
		 * @return The color the feature should be drawn with.
		 */
		protected Color colorForFeature(SimpleFeature feature) {
			if (attrNum == -1) {
				return attrMaxColor;
			}
			double attrval = ((Number) (feature.getAttribute(attrNum))).doubleValue();
			double percentage = (attrval - attrMin) / (attrMax - attrMin);
			return ColorBlend.mixColors(attrMinColor, attrMaxColor, (float) percentage);
		}



		/**
		 * Returns the time in milliseconds when the cursor was last moved.
		 * @return time in milliseconds.
		 */
		public long getLastCursorMoveTime() {
			return lastCursorMoveTime;
		}

		/**
		 * 
		 * @param event
		 */

		public void moved(PositionEvent event) {
			if (event == null || event.getPosition() == null) {
				return;
			}

			lastCursorMoveTime = System.currentTimeMillis();
			mousePosition = event.getPosition();
		}




		/**
		 * @return The original coordinate reference system this shapefile was in.
		 */
		public CoordinateReferenceSystem getCRS() {
			return this.originalCRS;
		}

		/** Get the feature at a certain location
		 * 
		 * @param latlon the feature at a position.
		 * @return The feature object.
		 */
		public SimpleFeature getFeatureAt(LatLon latlon) {
			return GeoToolsUtils.getFeatureAt(latlon, featureSource, originalCRS);
		}

		/**
		 * 
		 * @return all SimpleFeatures
		 */
//		public SimpleFeature[] getFeatures() {
//			return features;
//		}

		/**
		 * 
		 * @return The feature source for this shapefile.
		 */
		public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {
			return featureSource;
		}

		/** Get the current mouse position.
		 *
		 * @return the current mouse position
		 */
		public Position getMousePosition() {
			return mousePosition;
		}

		/**
		 * 
		 * @return The schema for this shapefile.
		 */
		public SimpleFeatureType getSchema() {
			return featureSource.getSchema();
		}

		/**
		 * 
		 * @return The boundaries of this shapefile.
		 * @throws java.io.IOException
		 */
		public Sector getSector() {
			//TODO:
			// Envelope env = map.getAreaOfInterest();
			Envelope env = map.getMaxBounds();
			double aspect = 1.0;//env.getWidth()/env.getHeight();
			double halfWidth = env.getWidth() * 0.5 * aspect;
			double halfHeight = env.getHeight() * 0.5;
			double cx = env.getMinX() + (env.getWidth() * 0.5);
			double cy = env.getMinY() + (env.getHeight() * 0.5);
			Sector unprojectedSector = Sector.fromDegrees(cy - halfHeight, cy + halfHeight, cx - halfWidth, cx + halfWidth);
			return unprojectedSector;
		}


		public void setAttrMinColor(Color attrMinColor) {
			this.attrMinColor = attrMinColor;
		}


		public void setAttrMaxColor(Color attrMaxColor) {
			this.attrMaxColor = attrMaxColor;
		}



		@Override
		public String toString() {
			return this.getName();
		}







	}
