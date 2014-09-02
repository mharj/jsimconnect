package flightsim.simconnect.data;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Helper class containting position of a point in three dimensionnal
 * space
 * @author lc0277
 *
 */
public class XYZ implements SimConnectData, Serializable {
	private static final long serialVersionUID = -2922269039547967440L;
	
	public double  x;   // degrees
    public double  y;  // degrees
    public double  z;   // feet   
    
	public XYZ(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public XYZ() {
		
	}

	public void read(ByteBuffer buffer) {
		x = buffer.getDouble();
		y = buffer.getDouble();
		z = buffer.getDouble();
	}
	
	public void write(ByteBuffer buffer) {
		buffer.putDouble(x);
		buffer.putDouble(y);
		buffer.putDouble(z);
	}
    
	/**
	 * Returns a value of this triple, index by position (0 = x, 1 = y, 2 = z)
	 * If <code>index</code> is out of bounds, return -1
	 * @param index
	 * @return value (x, y or z)
	 * @since 0.4
	 */
	double get(int index) {
		if (index == 0) return x;
		if (index == 1) return y;
		if (index == 2) return z;
		return -1;
	}
	
	@Override
	public String toString() {
		return x + ", " + y + ", " + z;
	}
	
	/**
	 * Set cartesian points coordinates from RADIANS lat,lon and alt
	 * @param lat
	 * @param lon
	 * @param alt
	 */
	public void setFromSpherical(double lat, double lon, double alt) {
		x = alt * Math.sin(lat) * Math.cos(lon);
		y = alt * Math.sin(lat) * Math.sin(lon);
		z = alt * Math.cos(lat);
		/*
		x = alt * Math.cos(lat) * Math.cos(lon);
		y = alt * Math.cos(lat) * Math.sin(lon);
		z = alt * Math.sin(lat);
		*/
	}

	/**
	 * Set cartesian points coordinates from DEGREES lat,lon and alt
	 */
	public void setFromSpherical(LatLonAlt lla, double earthRadius) {
		double lat = Math.toRadians(lla.latitude);
		double lon = Math.toRadians(lla.longitude);
		double alt = earthRadius + lla.altitude;
		x = alt * Math.sin(lat) * Math.cos(lon);
		y = alt * Math.sin(lat) * Math.sin(lon);
		z = alt * Math.cos(lat);
	}
	
	public double dist() {
		return Math.sqrt(x*x + y*y + z*z);
	}

	public double dist(double dx, double dy, double dz) {
		return Math.sqrt((x-dx)*(x-dx) + (y-dy)*(y-dy) + (z-dz)*(z-dz));
	}
	
	public double dist(XYZ p) {
		return dist(p.x, p.y, p.z);
	}

	public void translate(double xx, double yy, double zz){
		x += xx;
		y += yy;
		z += zz;
	}
	

	public void rotateX(double a) {
		double newx = x;
		double newy = Math.cos(a) * y - Math.sin(a) * z;
		double newz = Math.sin(a) * y + Math.cos(a) * z;
		this.x = newx;
		this.y = newy;
		this.z = newz;
	}

	public void rotateY(double a) {
		double newx = Math.cos(a) * x + Math.sin(a) * z;
		double newy = y;
		double newz = -Math.sin(a) * x + Math.cos(a) * z;
		this.x = newx;
		this.y = newy;
		this.z = newz;
	}

	public void rotateZ(double a) {
		double newx = Math.cos(a) * x - Math.sin(a) * y;
		double newy = Math.sin(a) * x + Math.cos(a) * y;
		double newz = z;
		this.x = newx;
		this.y = newy;
		this.z = newz;
	}

	public void projectionFrustum(double l, double r, double b, double t, double n, double f) {
		double newx = (2 * n) / (r - l) * x + ((r+l)/(r-l)) * z;
		double newy = (2 * n) / (t - b) * y + ((t+b)/(t-b)) * z;
		double newz = (-(f+n)/(f-n)) * z;
		this.x = newx;
		this.y = newy;
		this.z = newz;
	}
	
	public void projection2(double xf, double yf, double xzf, double yzf) {
		double newx = xf * x + xzf * z;
		double newy = yf * y + yzf * z;
		double newz = z;
		this.x = newx;
		this.y = newy;
		this.z = newz;
	}

	public XYZ clone() {
		return new XYZ(x,y,z);
	}


}
