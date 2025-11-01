#!/usr/bin/env bash

# Run with data directory (processes all CSV files)
# java -cp "classes;lib/*" edu.hofstra.csc17.proj.soclog.Main data # windows
java -cp "classes:lib/*" edu.hofstra.csc17.proj.soclog.Main data # linux/macOS

# Alternative: Run with no arguments (defaults to data directory)
# java -cp "classes:lib/*" edu.hofstra.csc17.proj.soclog.Main