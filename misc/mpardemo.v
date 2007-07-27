module half_add(a, b, s, c);
  input a, b;
  output s, c;
  assign s = a ^ b;
  assign c = a & b;
endmodule /* half_add */

module add(a, b, ci, o, co);
   input a, b, ci;
   output o, co;
   wire c1, c2, x;
   half_add h1(a, b,  x, c1);
   half_add h2(x, ci, o, c2);
   assign co = c1 | c2;
endmodule /* add */

module main(a, b, ci, out);

   input  [7:0] a;
   input  [7:0] b;
   input        ci;
   output [8:0] out;
   wire   [7:0] c;

   add a1(a[0], b[0], ci,   out[0], c[0]);
   add a2(a[1], b[1], c[0], out[1], c[1]);
   add a3(a[2], b[2], c[1], out[2], c[2]);
   add a4(a[3], b[3], c[2], out[3], c[3]);
   add a5(a[4], b[4], c[3], out[4], c[4]);
   add a6(a[5], b[5], c[4], out[5], c[5]);
   add a7(a[6], b[6], c[5], out[6], c[6]);
   add a8(a[7], b[7], c[6], out[7], out[8]);

endmodule /* main */

