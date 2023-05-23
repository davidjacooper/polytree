package com.example.test;


public class TestClassA
{
}

abstract class TestClassB extends TestClassA
{
}

final class TestClassC implements TestInterfaceX
{
}

abstract class TestClassD extends TestClassB implements TestInterfaceX, TestInterfaceY
{
}

interface TestInterfaceX
{
}

interface TestInterfaceY
{
}

interface TestInterfaceZ extends TestInterfaceX, TestInterfaceY
{
}
