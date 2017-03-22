import Utilities.Utils;

public class Test {
	public static void main(String args[]) {
		int a, b, c, d, e, f, g, h;
		a = 1;
		b = 2;
		c = 3;
		d = 4;
		e = 5;
		f = 6;
		g = 7;
		h = 8;

		while ((b / 4 + 5) < 8) {
			a = a * 7 + 9;
			if (c < d) {
				g = (g - 5) * h;
				while (g > h) {
					h = h + 1;
				}
				g = g + h;
			} else {
				if (c >= d) {
					e = f * f * 7 - 2;
					while ((d - 7) != e) {
						d = d - 1;
						e = e + 1;
					}
					f = f * e;
				} else {
					g = 725;
					while ((d - 8) != e) {
						d = d - 1;
						e = e + 1;
					}
					f = (g * f) / 4;
				}
				g = g + h;
			}

		}
		c = a * d;
		h = g + h - 7;
		e = f + b * c;

		Utils.SOPln(a);
		Utils.SOPln(b);
		Utils.SOPln(c);
		Utils.SOPln(d);
		Utils.SOPln(e);
		Utils.SOPln(f);
		Utils.SOPln(g);
		Utils.SOPln(h);
	}
}
