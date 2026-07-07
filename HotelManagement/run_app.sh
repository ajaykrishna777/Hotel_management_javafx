#!/bin/bash
cd /Users/jsreshta/Documents/HotelManagement
java -cp "target/classes:$(find ~/.m2/repository/org/openjfx -name "*.jar" | tr "
" ":")" --add-opens java.base/java.lang=ALL-UNNAMED --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED main.MainApp
