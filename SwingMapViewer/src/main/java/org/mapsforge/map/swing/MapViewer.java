/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright © 2014 Christian Pesch
 * Copyright © 2014 Ludwig M Brinckmann
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.swing;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.swing.controller.MapViewComponentListener;
import org.mapsforge.map.swing.controller.MouseEventListener;
import org.mapsforge.map.swing.util.JavaUtilPreferences;
import org.mapsforge.map.swing.view.MainFrame;
import org.mapsforge.map.swing.view.MapView;
import org.mapsforge.map.swing.view.WindowCloseDialog;

import java.io.File;
import java.util.prefs.Preferences;

public final class MapViewer {
	private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

    /**
     * Starts the mapviewer.
     * @param args command line args: optionally the name of the map file to load
     */
	public static void main(String[] args) {
		MapView mapView = createMapView();
        String mapFileName = args.length > 0 ? args[0] : "../../germany.map";
        BoundingBox boundingBox = addLayers(mapView, new File(mapFileName));

		PreferencesFacade preferencesFacade = new JavaUtilPreferences(Preferences.userNodeForPackage(MapViewer.class));
		Model model = mapView.getModel();
		model.init(preferencesFacade);

		MainFrame mainFrame = new MainFrame();
		mainFrame.add(mapView);
		mainFrame.addWindowListener(new WindowCloseDialog(mainFrame, model, preferencesFacade));
		mainFrame.setVisible(true);

		byte zoomLevel = 12;
		if (model.mapViewDimension.getDimension() != null) {
			// this is a fix for an apparent race condition where the update to the mapViewDimension that
			// should be delivered through the MapViewComponentListener has not arrived yet. If the
			// mapViewDimension is not set, it would result in a NPE
			zoomLevel = LatLongUtils.zoomForBounds(model.mapViewDimension.getDimension(), boundingBox, model.displayModel.getTileSize());
		}
        model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoomLevel));
	}

	private static BoundingBox addLayers(MapView mapView, File mapFile) {
		Layers layers = mapView.getLayerManager().getLayers();
		TileCache tileCache = createTileCache();

		// layers.add(createTileDownloadLayer(tileCache, mapView.getModel().mapViewPosition));
        TileRendererLayer tileRendererLayer = createTileRendererLayer(tileCache, mapView.getModel().mapViewPosition, mapFile);
        BoundingBox boundingBox = tileRendererLayer.getMapDatabase().getMapFileInfo().boundingBox;
        layers.add(tileRendererLayer);
		// layers.add(new TileGridLayer(GRAPHIC_FACTORY));
		// layers.add(new TileCoordinatesLayer(GRAPHIC_FACTORY));
        return boundingBox;
	}

	private static MapView createMapView() {
		MapView mapView = new MapView();
		mapView.getMapScaleBar().setVisible(true);
		mapView.getFpsCounter().setVisible(true);
		mapView.addComponentListener(new MapViewComponentListener(mapView, mapView.getModel().mapViewDimension));

		MouseEventListener mouseEventListener = new MouseEventListener(mapView.getModel());
		mapView.addMouseListener(mouseEventListener);
		mapView.addMouseMotionListener(mouseEventListener);
		mapView.addMouseWheelListener(mouseEventListener);

		return mapView;
	}

	private static TileCache createTileCache() {
		TileCache firstLevelTileCache = new InMemoryTileCache(64);
		File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
		TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
		return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
	}

    @SuppressWarnings("unused")
	private static Layer createTileDownloadLayer(TileCache tileCache, MapViewPosition mapViewPosition) {
		TileSource tileSource = OpenStreetMapMapnik.INSTANCE;
		TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(tileCache, mapViewPosition, tileSource,
				GRAPHIC_FACTORY);
		tileDownloadLayer.start();
		return tileDownloadLayer;
	}

    @SuppressWarnings("unused")
	private static TileRendererLayer createTileRendererLayer(TileCache tileCache, MapViewPosition mapViewPosition, File mapFile) {
		boolean isTransparent = false;
	    TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapViewPosition, isTransparent, GRAPHIC_FACTORY);
		tileRendererLayer.setMapFile(mapFile);
		tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.OSMARENDER);
		return tileRendererLayer;
	}

	private MapViewer() {
		throw new IllegalStateException();
	}
}
