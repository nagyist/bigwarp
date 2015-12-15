package bdv.viewer;

import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.models.CoordinateTransform;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.state.ViewerState;
import bigwarp.landmarks.LandmarkTableModel;

public class BigWarpOverlay {
	
	/** The viewer state. */
	private ViewerState state;
	
	private BigWarpViewerPanel viewer;
	
	protected LandmarkTableModel landmarkModel;
	
	protected CoordinateTransform estimatedXfm;
	
	protected boolean isTransformed = false;
	protected final boolean isMoving;
	protected final boolean is3d;
	
	/** The transform for the viewer current viewpoint. */
	private final AffineTransform3D transform = new AffineTransform3D();
	
	public BigWarpOverlay( final BigWarpViewerPanel viewer, LandmarkTableModel landmarkModel )
	{
		this.viewer = viewer;
		this.landmarkModel = landmarkModel;

		if( landmarkModel.getNumdims() == 3 )
			is3d = true;
		else
			is3d = false;

		isMoving = viewer.getIsMoving();
	}

	public void paint( final Graphics2D g ) 
	{
//		if( viewer.isMoving )
//		{
//			System.out.println("painting moving image overlay");
//		}

		/*
		 * Collect current view.
		 */
		state.getViewerTransform( transform );

		// Save graphic device original settings
		final Composite originalComposite = g.getComposite();
		final Stroke originalStroke = g.getStroke();
		final Color originalColor = g.getColor();

		/*
		 * Draw spots.
		 */
		if ( viewer.getSettings().areLandmarksVisible() )
		{

			final double radiusRatio = ( Double ) viewer.getSettings().get( 
					BigWarpViewerSettings.KEY_SPOT_RADIUS_RATIO );
			
			final double radius = viewer.getSettings().getSpotSize();

			Color color;
			Stroke stroke;
			stroke = BigWarpViewerSettings.NORMAL_STROKE;
			
			FontMetrics fm = null;
			int fonthgt = 0;
			Color textBoxColor = null;
			if ( viewer.getSettings().areNamesVisible() )
			{
				fm = g.getFontMetrics( g.getFont() );
				fonthgt = fm.getHeight();
				textBoxColor = Color.BLACK;
			}
			
			for( int index = 0; index < landmarkModel.getRowCount(); index++ )
			{
				Double[] spot; // = landmarkModel.getPoints().get( index );

				if ( landmarkModel.isActive( index ) )
					color = viewer.getSettings().getSpotColor();
				else
					color = viewer.getSettings().getInactiveSpotColor();

				g.setColor( color );
				g.setStroke( stroke );

				double x = 0.0, y = 0.0, z = 0.0;
				spot = landmarkModel.getPoints( isMoving ).get( index );

				// if this point is not set, don't render it.
				if ( Double.isInfinite( spot[ 0 ] ) )
					continue;

				// if the viewer is moving but transformed, render the points
				// at the location of the fixed point
				if ( isMoving )
				{
					if ( viewer.isInFixedImageSpace() && landmarkModel.isWarpedPositionChanged( index ) )
						spot = landmarkModel.getWarpedPoints().get( index );
					else if( viewer.isInFixedImageSpace() )
						spot = landmarkModel.getPoints( false ).get( index );
				}

				// have to do this song and dance because globalCoords should be a length-3 array
				// all the time with z=0 if we're in a 2d
				x = spot[ 0 ];
				y = spot[ 1 ];
				if ( is3d )
					z = spot[ 2 ];

				final double[] globalCoords = new double[] { x, y, z };
				final double[] viewerCoords = new double[ 3 ];
				transform.apply( globalCoords, viewerCoords );

				// final double rad = radius * transformScale * radiusRatio;
				final double rad = radius * radiusRatio;
				final double zv = viewerCoords[ 2 ];
				final double dz2 = zv * zv;

				if ( dz2 < rad * rad )
				{
					final double arad = Math.sqrt( rad * rad - dz2 );
					
					// vary size
					g.fillOval( ( int ) ( viewerCoords[ 0 ] - arad ), 
								( int ) ( viewerCoords[ 1 ] - arad ), 
								( int ) ( 2 * arad ), ( int ) ( 2 * arad ) );
					
					if ( viewer.getSettings().areNamesVisible() )
					{
						final int tx = ( int ) ( viewerCoords[ 0 ] + arad + 5 );
						final int ty = ( int ) viewerCoords[ 1 ];
						
						String name = landmarkModel.getNames().get(index);
						int strwidth = fm.stringWidth( name );
						
						textBoxColor = new Color( color.getRed(), color.getGreen(), color.getBlue(), 128 );
						
						g.setColor( textBoxColor );
						g.fillRect( tx - 1, ty - fonthgt + 2, strwidth + 2, fonthgt);
						
						g.setColor( Color.BLACK );
						g.drawString( name, tx, ty );
						
					}
				}
				
			}
		}

		// Restore graphic device original settings
		g.setComposite( originalComposite );
		g.setStroke( originalStroke );
		g.setColor( originalColor );
	}


	/**
	 * Update data to show in the overlay.
	 */
	public void setViewerState( final ViewerState state )
	{
		this.state = state;
	}
	
	public void setEstimatedTransform( ThinPlateR2LogRSplineKernelTransform estimatedXfm )
	{
		this.estimatedXfm = estimatedXfm.deepCopy();
	}
	
	public boolean getIsTransformed()
	{
		return isTransformed;
	}
	
	public void setIsTransformed( boolean isTransformed )
	{
		this.isTransformed = isTransformed;
	}

}

