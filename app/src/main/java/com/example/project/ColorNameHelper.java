package com.example.project;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class ColorNameHelper {

    public static class ColorEntry {
        public String name;
        public String hex;
        public int r, g, b;

        public ColorEntry(String name, String hex) {
            this.name = name;
            this.hex = hex;
            int color = Color.parseColor(hex);
            this.r = Color.red(color);
            this.g = Color.green(color);
            this.b = Color.blue(color);
        }
    }

    private static final List<ColorEntry> STANDARD_COLORS = new ArrayList<>();

    static {
        String[] blackHexes = {
            "#000000", "#010101", "#020202", "#030303", "#040404", "#050505", "#060606", "#070707", "#080808", "#090909",
            "#0A0A0A", "#0B0B0B", "#0C0C0C", "#0D0D0D", "#0E0E0E", "#0F0F0F", "#101010", "#111111", "#121212", "#131313",
            "#141414", "#151515", "#161616", "#171717", "#181818", "#191919", "#1A1A1A", "#1B1B1B", "#1C1C1C", "#1D1D1D",
            "#1E1E1E", "#1F1F1F", "#202020", "#212121", "#222222", "#0A0505", "#050A05", "#05050A", "#100B0B", "#0B100B",
            "#0B0B10", "#151010", "#101510", "#101015", "#1A1515", "#151A15", "#15151A", "#201B1B", "#1B201B", "#1B1B20",
            "#0A0A0E", "#05080A", "#020406", "#080604", "#040804", "#111417", "#171411", "#141711", "#000005", "#050000"
        };

        String[] whiteHexes = {
                // STRICT WHITES: High brightness only. Cutoff is at #F5.
                "#FFFFFF", "#FEFEFE", "#FDFDFD", "#FCFCFC", "#FBFBFB", "#FAFAFA", "#F9F9F9", "#F8F8F8", "#F7F7F7", "#F6F6F6",
                "#F5F5F5", "#FFFAFA", "#F0FFF0", "#F5FFFA", "#F0FFFF", "#F0F8FF", "#F8F8FF", "#FFF5EE", "#FDF5E6", "#FFFAF0",
                "#FFFFF0", "#FAEBD7", "#FAF0E6", "#FFF0F5", "#FFE4E1", "#FFFDD0", "#F4F6F7", "#FDFCF0", "#FCFCFA", "#FAFCFC",
                "#FCFAFC", "#FBFEF9", "#F9FBFE", "#FEF9FB", "#FFFDFD", "#FDFFFD", "#FDFFFF", "#FFFFFD", "#FCFFFC"
        };

        String[] greyHexes = {
                // STRICT GREYS: Starts exactly at #EEEEEE down to #222222.
                // Removing all ultra-light greys forces shadows to snap to "White".
                "#EEEEEE", "#EBEBEB", "#E8E8E8", "#E5E5E5", "#E2E2E2", "#DFDFDF", "#DCDCDC", "#D9D9D9", "#D6D6D6", "#D3D3D3",
                "#D0D0D0", "#CDCDCD", "#CACACA", "#C7C7C7", "#C4C4C4", "#C1C1C1", "#BEBEBE", "#BBBBBB", "#B8B8B8", "#B5B5B5",
                "#B2B2B2", "#AFAFAF", "#ACACAC", "#A9A9A9", "#A6A6A6", "#A3A3A3", "#A0A0A0", "#9D9D9D", "#9A9A9A", "#979797",
                "#949494", "#919191", "#8E8E8E", "#8B8B8B", "#888888", "#858585", "#828282", "#7F7F7F", "#7C7C7C", "#797979",
                "#767676", "#737373", "#707070", "#6D6D6D", "#6A6A6A", "#676767", "#646464", "#616161", "#5E5E5E", "#5B5B5B",
                "#585858", "#555555", "#525252", "#4F4F4F", "#4C4C4C", "#494949", "#464646", "#434343", "#404040", "#3D3D3D",
                "#3A3A3A", "#373737", "#343434", "#313131", "#2E2E2E", "#2B2B2B", "#282828", "#252525", "#222222", "#36454F",
                "#708090", "#B2BEB5", "#8E9CB2", "#71797E", "#778899", "#818589", "#899499", "#555D50"
        };

        String[] redHexes = {
            "#FF0000", "#FE0000", "#FD0000", "#FC0000", "#FB0000", "#FA0000", "#F90000", "#F80000", "#F70000", "#F60000",
            "#F50000", "#F00000", "#E50000", "#E00000", "#D50000", "#D00000", "#C50000", "#C00000", "#B50000", "#B00000",
            "#A50000", "#A00000", "#950000", "#900000", "#8B0000", "#850000", "#800000", "#750000", "#700000", "#650000",
            "#600000", "#550000", "#500000", "#450000", "#400000", "#350000", "#300000", "#FF0A0A", "#FF1414", "#FF1E1E",
            "#FF2828", "#FF3232", "#FF3C3C", "#FF4646", "#FF5050", "#FF5A5A", "#FF6464", "#FF6E6E", "#FF7878", "#FF8282",
            "#E61919", "#CC0000", "#B30000", "#990000", "#800020", "#92000A", "#960018", "#DC143C", "#E0115F", "#D2042D",
            "#CB4154", "#FF2400", "#B22222", "#CE2029", "#ED2939", "#C21E56", "#E84A5F", "#FF0800", "#7A0000", "#A40000",
            "#8A0303", "#5C0000", "#D92121", "#C41E1E", "#B01A1A", "#9B1717", "#871414", "#721111", "#F22E2E", "#E82727",
            "#DE2121", "#D41A1A", "#CA1414", "#FF1A1A", "#FF3333", "#FF4D4D", "#FF6666", "#CC1111", "#BB1111", "#AA1111",
            "#991111", "#881111", "#771111", "#661111", "#EE2222", "#DD2222", "#CC2222", "#BB2222", "#AA2222", "#992222"
        };

        String[] greenHexes = {
            "#00FF00", "#00FE00", "#00FD00", "#00FC00", "#00FB00", "#00FA00", "#00F900", "#00F800", "#00F500", "#00F000",
            "#00E500", "#00E000", "#00D500", "#00D000", "#00C500", "#00C000", "#00B500", "#00B000", "#00A500", "#00A000",
            "#009500", "#009000", "#008B00", "#008500", "#008000", "#007500", "#007000", "#006500", "#006000", "#005500",
            "#005000", "#004500", "#004000", "#003500", "#003000", "#002500", "#002000", "#0AFF0A", "#14FF14", "#1EFF1E",
            "#28FF28", "#32FF32", "#3CFF3C", "#46FF46", "#50FF50", "#5AFF5A", "#64FF64", "#6EFF6E", "#78FF78", "#82FF82",
            "#8CFF8C", "#96FF96", "#A0FFA0", "#006400", "#228B22", "#50C878", "#00A86B", "#808000", "#6B8E23", "#9DC183",
            "#98FF98", "#2E8B57", "#008080", "#01796F", "#39FF14", "#7FFF00", "#ADFF2F", "#32CD32", "#90EE90", "#00FF7F",
            "#3CB371", "#8FBC8F", "#00FA9A", "#1ABC9C", "#16A085", "#27AE60", "#2ECC71", "#00CC66", "#00994D", "#006633",
            "#00331A", "#556B2F", "#A4C639", "#8A9A5B", "#11AA11", "#11BB11", "#11CC11", "#11DD11", "#11EE11", "#22AA22",
            "#22BB22", "#22CC22", "#22DD22", "#22EE22", "#33AA33", "#33BB33", "#33CC33", "#33DD33", "#33EE33", "#44AA44",
            "#44BB44", "#44CC44", "#44DD44", "#44EE44", "#194D19", "#267326", "#339933", "#40BF40", "#4CE64C", "#59B359"
        };

        String[] blueHexes = {
            "#0000FF", "#0000FE", "#0000FD", "#0000FC", "#0000FB", "#0000FA", "#0000F5", "#0000F0", "#0000E5", "#0000E0",
            "#0000D5", "#0000D0", "#0000C5", "#0000C0", "#0000B5", "#0000B0", "#0000A5", "#0000A0", "#000095", "#000090",
            "#00008B", "#000085", "#000080", "#000075", "#000070", "#000065", "#000060", "#000055", "#000050", "#000045",
            "#000040", "#000035", "#000030", "#000025", "#000020", "#0A0AFF", "#1414FF", "#1E1EFF", "#2828FF", "#3232FF",
            "#3C3CFF", "#4646FF", "#5050FF", "#5A5AFF", "#6464FF", "#6E6EFF", "#7878FF", "#8282FF", "#8C8CFF", "#9696FF",
            "#A0A0FF", "#4169E1", "#0047AB", "#0F52BA", "#1560BD", "#4682B4", "#007BA7", "#007FFF", "#87CEEB", "#89CFF0",
            "#00FFFF", "#40E0D0", "#7FFFD4", "#1E90FF", "#00BFFF", "#ADD8E6", "#B0E0E6", "#5F9EA0", "#6495ED", "#3498DB",
            "#2980B9", "#3366FF", "#0033CC", "#002266", "#3399FF", "#66B2FF", "#99CCFF", "#CCE5FF", "#0055A4", "#1034A6",
            "#0080FF", "#088F8F", "#191970", "#000033", "#00004D", "#000066", "#000099", "#0A1128", "#1C2841", "#212A3E",
            "#2B3A55", "#001F3F", "#0B0C10", "#1F2833", "#1A2421", "#2C3E50", "#0A246A", "#14338A", "#1F42AA", "#2952CA",
            "#3361EA", "#1111AA", "#1111BB", "#1111CC", "#1111DD", "#1111EE", "#2222AA", "#2222BB", "#2222CC", "#2222DD",
            "#2222EE", "#3333AA", "#3333BB", "#3333CC", "#3333DD", "#3333EE", "#4444AA", "#4444BB", "#4444CC", "#4444DD",
            "#1A1A4D", "#262673", "#333399", "#4040BF", "#4C4CE6", "#5959B3", "#001133", "#002244", "#003355", "#004466"
        };

        String[] yellowHexes = {
            "#FFFF00", "#FEFE00", "#FDFD00", "#FCFC00", "#FBFB00", "#FAFA00", "#F9F900", "#F8F800", "#F5F500", "#F0F000",
            "#E5E500", "#E0E000", "#D5D500", "#D0D000", "#C5C500", "#C0C000", "#B5B500", "#B0B000", "#A5A500", "#A0A000",
            "#959500", "#909000", "#8B8B00", "#858500", "#808000", "#757500", "#707000", "#656500", "#606000", "#FFFF0A",
            "#FFFF14", "#FFFF1E", "#FFFF28", "#FFFF32", "#FFFF3C", "#FFFF46", "#FFFF50", "#FFFF5A", "#FFFF64", "#FFFF6E",
            "#FFFF78", "#FFFF82", "#FFFF8C", "#FFFF96", "#FFFFA0", "#FFFFE0", "#FFDB58", "#FFBF00", "#FFD700", "#FFF700",
            "#F4C430", "#FBEC5D", "#FFFF66", "#FFFF99", "#FFFFCC", "#E6E600", "#CCCC00", "#B3B300", "#999900", "#F1C40F",
            "#F39C12", "#DAA520", "#B8860B", "#EEE8AA", "#F0E68C", "#BDB76B", "#FFFACD", "#FAFAD2", "#EEDC82", "#F3E5AB",
            "#EEEE11", "#DDDD11", "#CCCC11", "#BBBB11", "#AAAA11", "#999911", "#888811", "#777711", "#666611", "#EEEE22",
            "#DDDD22", "#CCCC22", "#BBBB22", "#AAAA22", "#999922", "#EEEE33", "#DDDD33", "#CCCC33", "#BBBB33", "#AAAA33",
            "#E6E619", "#E6E633", "#E6E64D", "#E6E666", "#E6E680", "#E6E699", "#E6E6B3", "#E6E6CC", "#CCCC19", "#CCCC33"
        };

        String[] orangeHexes = {
            "#FFA500", "#FF9D00", "#FF9500", "#FF8D00", "#FF8500", "#FF7D00", "#FF7500", "#FF6D00", "#FF6500", "#FF5D00",
            "#FF5500", "#FF4D00", "#FF4500", "#FF3D00", "#FF3500", "#FF8C00", "#CC5500", "#F28500", "#FF7518", "#FF7F50",
            "#FFE5B4", "#FBCEB1", "#B7410E", "#E2725B", "#ED9121", "#F2981B", "#E67E22", "#D35400", "#FF9933", "#FFB366",
            "#FFCC99", "#E65C00", "#B34700", "#993D00", "#CD5C5C", "#FF6347", "#FF8833", "#E56717", "#FFDAB9", "#E68A00",
            "#CC7A00", "#B36B00", "#995C00", "#804D00", "#663D00", "#FF9900", "#FF8800", "#FF7700", "#FF6600", "#FF5500",
            "#FF9911", "#FF9922", "#FF9933", "#FF9944", "#FF9955", "#FF9966", "#FF9977", "#FF9988", "#FF8811", "#FF8822",
            "#FF8833", "#FF8844", "#FF8855", "#FF8866", "#FF8877", "#FF7711", "#FF7722", "#FF7733", "#FF7744", "#FF7755",
            "#D97B00", "#C46F00", "#B06400", "#9B5800", "#874D00", "#F28A00", "#E88400", "#DE7E00", "#D47800", "#CA7200",
            "#FFB84D", "#FFA31A", "#FF8F00", "#E68100", "#CC7300", "#B36500", "#995600", "#804800", "#663A00", "#4D2B00"
        };

        String[] purpleHexes = {
            "#800080", "#7D007D", "#7A007A", "#750075", "#700070", "#6B006B", "#650065", "#600060", "#5B005B", "#550055",
            "#500050", "#4B004B", "#450045", "#400040", "#3B003B", "#350035", "#300030", "#2B002B", "#250025", "#200020",
            "#8A0A8A", "#941494", "#9E1E9E", "#A828A8", "#B232B2", "#BC3CBC", "#C646C6", "#D050D0", "#DA5ADA", "#E464E4",
            "#EE6EEE", "#EE82EE", "#8E4585", "#614051", "#9966CC", "#E6E6FA", "#C8A2C8", "#E0B0FF", "#DA70D6", "#C54B8C",
            "#CCCCFF", "#4B0082", "#9370DB", "#8A2BE2", "#9400D3", "#9932CC", "#BA55D3", "#D8BFD8", "#483D8B", "#6A5ACD",
            "#7B68EE", "#8E44AD", "#9B59B6", "#CC99FF", "#B266FF", "#9933FF", "#7F00FF", "#6600CC", "#4C0099", "#330066",
            "#512E5F", "#4A235A", "#AA11AA", "#BB11BB", "#CC11CC", "#DD11DD", "#EE11EE", "#FF11FF", "#AA22AA", "#BB22BB",
            "#CC22CC", "#DD22DD", "#EE22EE", "#FF22FF", "#AA33AA", "#BB33BB", "#CC33CC", "#DD33DD", "#EE33EE", "#FF33FF",
            "#660066", "#730073", "#8C008C", "#990099", "#A600A6", "#B300B3", "#BF00BF", "#CC00CC", "#D900D9", "#E600E6",
            "#4D004D", "#590059", "#A853A8", "#B566B5", "#C27AC2", "#CF8FCF", "#DCA3DC", "#E9B8E9", "#F6CCF6", "#FFE0FF"
        };

        String[] pinkHexes = {
            "#FFC0CB", "#FFB6C1", "#FF69B4", "#FF10F0", "#FF00FF", "#C154C1", "#FF007F", "#DCAE96", "#FA8072", "#DE5D83",
            "#FC8EAC", "#FF1493", "#DB7093", "#F08080", "#FF33CC", "#FF66B2", "#FF99CC", "#FFCCE5", "#E5A9A9", "#F4C2C2",
            "#F8B878", "#C71585", "#FF00CC", "#FF3399", "#DDA0DD", "#FF77FF", "#FF80DF", "#FF99E6", "#FFB3EC", "#FFCCF2",
            "#FFE6F9", "#E600E6", "#CC00CC", "#B300B3", "#990099", "#800080", "#E673E6", "#E64DE6", "#E626E6", "#CC66CC",
            "#CC33CC", "#CC00CC", "#B34DB3", "#B31AB3", "#FF4DA6", "#FF1A8C", "#E60073", "#CC0066", "#B30059", "#99004D",
            "#FF66B3", "#FF3399", "#FF0080", "#E60073", "#CC0066", "#B30059", "#99004D", "#FF99CC", "#FF66B3", "#FF3399",
            "#FF1199", "#FF22AA", "#FF33BB", "#FF44CC", "#FF55DD", "#FF66EE", "#FF77FF", "#EE1188", "#EE2299", "#EE33AA",
            "#EE44BB", "#EE55CC", "#EE66DD", "#EE77EE", "#DD1177", "#DD2288", "#DD3399", "#DD44AA", "#DD55BB", "#DD66CC",
            "#FF88CC", "#FF99DD", "#FFAAEE", "#FFBBFF", "#EE88BB", "#EE99CC", "#EEAADD", "#EEBBEE", "#DD88AA", "#DD99BB",
            "#DDAACC", "#DDBBDD", "#CC8899", "#CC99AA", "#CCAABB", "#CCBBCC", "#BB8888", "#BB9999", "#BBAAAA", "#BBBBBB"
        };

        String[] brownHexes = {
            "#A52A2A", "#5C4033", "#7B3F00", "#493D26", "#C04000", "#954535", "#9A463D", "#635147", "#8B4513", "#A0522D",
            "#D2691E", "#CD853F", "#3D2B1F", "#6F4E37", "#834333", "#5E3A24", "#4A3018", "#795C34", "#3B2F2F", "#51361A",
            "#654321", "#8B5A2B", "#8A3324", "#5C3A21", "#8E4A23", "#704214", "#C19A6B", "#D2B48C", "#C3B091", "#483C32",
            "#E5AA70", "#C2B280", "#F5DEB3", "#D4C4A8", "#C4A484", "#B99470", "#A67B5B", "#D8C3A5", "#D9B99B", "#5C3317",
            "#855E42", "#87421F", "#8E2323", "#A62A2A", "#BC8F8F", "#C76114", "#D2B48C", "#E9C2A6", "#8A2BE2", "#A52A2A",
            "#B22222", "#CD5C5C", "#D2691E", "#E9967A", "#F4A460", "#CD853F", "#A0522D", "#8B4513", "#D2B48C", "#DEB887",
            "#F5DEB3", "#F4A460", "#D2691E", "#CD853F", "#A0522D", "#8B4513", "#D2B48C", "#DEB887", "#F5DEB3", "#F4A460",
            "#4D2600", "#663300", "#804000", "#994D00", "#B35900", "#CC6600", "#E67300", "#4D1A00", "#662200", "#802B00",
            "#993300", "#B33C00", "#CC4400", "#E64D00", "#4D0D00", "#661100", "#801500", "#991A00", "#B31F00", "#CC2400",
            "#593E26", "#66472C", "#735031", "#805937", "#8D623C", "#996B42", "#A67447", "#B37D4D", "#BF8652", "#CC8F58"
        };

        for (String hex : blackHexes)  STANDARD_COLORS.add(new ColorEntry("Black", hex));
        for (String hex : whiteHexes)  STANDARD_COLORS.add(new ColorEntry("White", hex));
        for (String hex : greyHexes)   STANDARD_COLORS.add(new ColorEntry("Grey", hex));
        for (String hex : redHexes)    STANDARD_COLORS.add(new ColorEntry("Red", hex));
        for (String hex : greenHexes)  STANDARD_COLORS.add(new ColorEntry("Green", hex));
        for (String hex : blueHexes)   STANDARD_COLORS.add(new ColorEntry("Blue", hex));
        for (String hex : yellowHexes) STANDARD_COLORS.add(new ColorEntry("Yellow", hex));
        for (String hex : orangeHexes) STANDARD_COLORS.add(new ColorEntry("Orange", hex));
        for (String hex : purpleHexes) STANDARD_COLORS.add(new ColorEntry("Purple", hex));
        for (String hex : pinkHexes)   STANDARD_COLORS.add(new ColorEntry("Pink", hex));
        for (String hex : brownHexes)  STANDARD_COLORS.add(new ColorEntry("Brown", hex));
    }

    public static ColorEntry getClosestColor(int pixel) {
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        ColorEntry closest = null;
        double minDistance = Double.MAX_VALUE;

        for (ColorEntry entry : STANDARD_COLORS) {
            double distance = Math.sqrt(Math.pow(r - entry.r, 2) + Math.pow(g - entry.g, 2) + Math.pow(b - entry.b, 2));
            if (distance < minDistance) {
                minDistance = distance;
                closest = entry;
            }
        }
        return closest;
    }
}
