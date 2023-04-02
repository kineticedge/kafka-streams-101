package io.kineticedge.ks101.admin;

import java.util.Map;

public class ListTopics {

    public static void main(String[] args) throws Exception {

        Options options = new Options();

        Admin admin = new Admin(options);

        System.out.println("topic,partitions,configs");

        admin.topics().forEach((k,v) -> {
            System.out.println(k + "," + v.getLeft() + "," + toString(v.getRight()));
        });

    }

    private static String toString(Map<String, Object> map) {

        StringBuilder builder = new StringBuilder();

        builder.append("\"");
        map.forEach((k, v) -> {
            // make sure any " is converted to "" to honor CSV parsing rules.
            builder.append(k + "=" + v.toString().replaceAll("\"", "\"\""));
            builder.append(",");
        });
        // feel free to clean this up with a more elegant approach.
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("\"");

        return builder.toString();
    }

}
