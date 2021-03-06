/*
 * $Id: WorldWindUtils.java 67 2008-12-08 20:23:28Z iovergard $
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
package bll.util;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceCircle;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

import gw.util.ProjectionUtils;

import java.awt.Color;
import java.util.ArrayList;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

/**
 *
 * @author Ian Overgard
 */
public class WorldWindUtils {

	static final double DEG_IN_METER = 111131.745d;

	/**
	 * Fly to a certain latlong position
	 * 
	 * @param upperleft
	 * @param lowerright
	 */
	public static void flyTo(LatLon upperleft, LatLon lowerright, WorldWindow canvas) {
		flyTo(new Sector(lowerright.getLatitude(), upperleft.getLatitude(), upperleft.getLongitude(), lowerright.getLongitude()), canvas);
	}


	public static void flyTo(Sector sect, WorldWindow canvas) {
		
		Position position = new Position(sect.getCentroid(),canvas.getView().getEyePosition().getAltitude());
		
		canvas.getView().setEyePosition(position);
		canvas.getView().firePropertyChange(AVKey.VIEW, null, canvas.getView());
	
	}

	public static double getCameraAltitude(LatLon upperleft, LatLon lowerright, double fovInRadians) {
		
		double majorDimension = 0; 
		LatLon dim = lowerright.subtract(upperleft);
		if (dim.getLatitude().getDegrees() > dim.getLongitude().getDegrees()) {
			majorDimension = dim.getLatitude().getDegrees();
		} else {
			majorDimension = dim.getLongitude().getDegrees();
		}

		double lengthOfDegreeInMeters = 111131.745;
		double extentInDegrees = majorDimension;
		double extentInMeters = extentInDegrees * lengthOfDegreeInMeters;
		return extentInMeters / Math.sin(fovInRadians);// + 2000;
	}

	
	public static SurfaceShape polygonFromFeature(SimpleFeature feature, CoordinateReferenceSystem crs)  {
		Geometry geometry = (Geometry)feature.getDefaultGeometry();
		Coordinate[] coords = geometry.getCoordinates();

		ArrayList<LatLon> vertices = new ArrayList<LatLon>();

		for (int i = 0; i < coords.length; i++) {
			double[] c = {coords[i].x, coords[i].y};
			vertices.add(ProjectionUtils.toLatLon(c, crs));
		}
		SurfaceShape surfacePolygon = new SurfacePolygon(vertices);
        surfacePolygon.setAttributes(getUnselectedAtts());
		surfacePolygon.setValue("ID", feature.getID());
		
		return surfacePolygon;
	}

	public static ArrayList<Renderable> multiPointFromFeature(SimpleFeature feature, CoordinateReferenceSystem crs)  {
		ArrayList<Renderable> renderables =  new ArrayList<Renderable>();
		MultiPoint mp = (MultiPoint) feature.getDefaultGeometry();
		int n = mp.getNumGeometries();
		for (int i = 0; i < n; i++) {
			Point poly = (Point) mp.getGeometryN(i);
			//Position p = GeoToolsUtils.getFeatureCenter(feature, baseShapeLayer.getCRS());
			Coordinate latLon = poly.getCoordinate();
			SurfaceShape shape = new SurfaceCircle(LatLon.fromDegrees(latLon.y, latLon.x),4);
			shape.setAttributes(getUnselectedAtts());
			shape.setValue("ID", feature.getID());
			renderables.add(shape);

			//renderables.add(new Sphere(new Vec4(latLon.x, latLon.y, latLon.z), 20));
		}
		return renderables;
	}


	public static SurfaceShape pointFromFeature(SimpleFeature feature, CoordinateReferenceSystem crs)  {
		Point mp = (Point) feature.getDefaultGeometry();
		Coordinate latLon = mp.getCoordinate();
		SurfaceShape shape = new SurfaceCircle(LatLon.fromDegrees(latLon.y, latLon.x),4);
		
		shape.setAttributes(getUnselectedAtts());
		shape.setValue("ID", feature.getID());
		return shape;
	}

	

	
	public static Polyline polylineFromFeature(SimpleFeature feature, CoordinateReferenceSystem crs, double elevation) {
		Geometry geometry = (Geometry)feature.getDefaultGeometry();
		Coordinate[] coords = geometry.getCoordinates();

		ArrayList<Position> vertices = new ArrayList<Position>();

		for (int i = 0; i < coords.length; i++) {
			double[] c = {coords[i].x, coords[i].y};
			LatLon ll = ProjectionUtils.toLatLon(c, crs);
			vertices.add(new Position(ll, elevation));
		}
		Polyline polyline = new Polyline();
		polyline.setPositions(vertices);
		polyline.setColor(Color.BLACK);
		polyline.setFollowTerrain(true);
		polyline.setAntiAliasHint(Polyline.ANTIALIAS_NICEST);
		polyline.setPathType(Polyline.LINEAR);
		polyline.setLineWidth(2);
		polyline.setClosed(false);
		polyline.setFilled(false);
		
		polyline.setValue("ID", feature.getID());
		return polyline;
	}

	public static SurfacePolygon polygonFromBoundingBox(double lat1, double long1, double lat2, double long2) {
		return null;
	}

	public static Sector sectorFromEnvelope(Envelope env) {
		return Sector.fromDegrees(env.getMinimum(1), env.getMaximum(1), env.getMinimum(0), env.getMaximum(0));
	}
	
	
	public static ShapeAttributes getSelectedAtts()
	{
		ShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setOutlineMaterial(Material.GREEN);
		attrs.setInteriorMaterial(Material.GREEN);
		
		return attrs;
	}
	
	
	public static ShapeAttributes getUnselectedAtts()
	{
		
		ShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setOutlineMaterial(Material.ORANGE);
		attrs.setInteriorMaterial(Material.YELLOW);
		attrs.setInteriorOpacity(0.5);
		return attrs;
		
	}
	
	
}
