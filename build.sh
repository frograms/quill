#!/bin/sh

sbt ++2.13.1 -Dquill.macro.log=false -Dquill.scala.version=2.13.1 -Dmodules=base package
sbt ++2.13.1 -Dquill.macro.log=false -Dquill.scala.version=2.13.1 -Dmodules=async package
