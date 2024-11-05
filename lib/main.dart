// lib/main.dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Native Print Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const PrintScreen(),
    );
  }
}

class PrintScreen extends StatefulWidget {
  const PrintScreen({super.key});

  @override
  State<PrintScreen> createState() => _PrintScreenState();
}

class _PrintScreenState extends State<PrintScreen> {
  static const platform = MethodChannel('com.example.native_print/print');
  bool isPrinting = false;

  Future<void> _printImage() async {
    try {
      setState(() {
        isPrinting = true;
      });

      // Load sample image from assets
      ByteData imageData = await rootBundle.load('assets/sample_invoice.png');
      Uint8List bytes = imageData.buffer.asUint8List();

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Print job sent ')),
      );
      // Send image data to native code
      await platform.invokeMethod('printImage', {
        'imageData': bytes,
      });
    } on PlatformException catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to print: ${e.message}')),
      );
    } finally {
      setState(() {
        isPrinting = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Native Print Demo'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (isPrinting)
              const CircularProgressIndicator()
            else
              ElevatedButton(
                onPressed: _printImage,
                child: const Text('Print Image'),
              ),
          ],
        ),
      ),
    );
  }
}
