#
# Jetty JSP Module
#

[depend]
servlet
jsp-impl/${jsp-impl}-jsp

[ini-template]
# JSP Configuration

# Select JSP implementation, choices are
#   glassfish : The reference implementation 
#               default in jetty <= 9.1
#   apache    : The apache version 
#               default jetty >= 9.2
jsp-impl=apache

# To use an non-jdk compiler for JSP compilation uncomment next line
# -Dorg.apache.jasper.compiler.disablejsr199=true
