pi=3.14159265;

// Make a RepRap teardrop with its axis along Z
// If truncated is true, chop the apex; if not, come to a point

// I stole this function from Erik...


module teardrop(r=1.5, h=20, truncateMM=0.5)
{
	union()
	{
		if(truncateMM > 0)
		{
			intersection()
			{
				translate([truncateMM,0,h/2]) 
					scale([1,1,h])
						cube([r*2.8275,r*2,1],center=true);
				scale([1,1,h]) 
						rotate([0,0,3*45])
							cube([r,r,1]);
			}
		} else
		{
			scale([1,1,h])
				rotate([0,0,3*45])
					cube([r,r,1]);
		}
		cylinder(r=r, h = h, $fn=20);
	}
}

module tooth_gap(height = 10, number_of_teeth = 11, inner_radius = 10, dr = 3,  angle=7)
{
	linear_extrude(height = 2*height, center = true, convexity = 10, twist = 0)
		polygon( points = [
			[pi*inner_radius/(2*number_of_teeth), 0],
			[-pi*inner_radius/(2*number_of_teeth), 0],
			[-2*dr *sin(angle) - pi*inner_radius/(2*number_of_teeth), 2*dr],
			[2*dr *sin(angle) + pi*inner_radius/(2*number_of_teeth), 2*dr],
		], convexity = 3);
}



module gear(height = 10, number_of_teeth = 11, inner_radius = 10, outer_radius = 12, angle=15)
{
	difference()
	{
		cylinder(h = height, r = outer_radius, centre = true);
		for(i = [0:number_of_teeth])
		{
		rotate([0, 0, i*360/number_of_teeth])
			translate([0, inner_radius, 0])
				tooth_gap(height = height, number_of_teeth = number_of_teeth, inner_radius = inner_radius, 
					dr =  outer_radius - inner_radius,  angle=angle);
		}
	}
}

// This is the actual pulley

difference()
{
	union()
	{
		translate([0,0,0])
			gear(height =13, number_of_teeth = 8, inner_radius = 4.5, outer_radius = 6.5, angle=40);
		translate([0,0,-3.75])
			cylinder(h = 7.5, r = 10,center=true,$fn=20);
	}
	
	cylinder(h = 30, r = 2.5,center=true,$fn=20);
	
	translate([4.75,0,-6])
		cube([2.7,5.5,10], center=true);
	translate([0,0,-3.75])
		rotate([0,90,0])
			teardrop(h = 20, r = 1.5,truncateMM=0.5);
}