#!/usr/bin/env groovy
/**
 * Author: alexanderjosef
 * Date: Apr 28, 2009
 * Time: 8:00:13 AM
 *
 * Groovy Script to process an image folder for an album locally.
 *
 */

import com.drew.metadata.Metadata
import com.drew.imaging.jpeg.JpegMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.exif.ExifDirectory
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import ch.unartig.imaging.ImagingHelper
import javax.imageio.stream.ImageInputStream
import javax.imageio.ImageReader
import javax.imageio.ImageIO

private static Integer displayPixelsLongerSide = 380; // used to be 484 for unartig.ch and the beginning of sportrait
private static Integer thumbnailPixelsLongerSide = 100;


println "Sportrait Importer"
if (args.length != 1) {
  println("Usage:")
  println("Importer.groovy [project directory]")
  println("No directory, quitting")
  System.exit(0);
}

println args

println args[0]

File imageFineFolder = new File(args[0], "fine")
// writer to the parent filder, relative to the fine folder
File importFile = new File("import.txt", imageFineFolder.parentFile)
def writer = importFile.newWriter()
int counter = 0

// make sure disp and thumb directory exist:
final File displayFolder = new File(imageFineFolder.parentFile, "display")
displayFolder.mkdirs()
final File thumbnailFolder = new File(imageFineFolder.parentFile, "thumbnail")
thumbnailFolder.mkdirs()



// go through all files with .JPG or .jpg in the folder:
imageFineFolder.eachFileMatch(~/.*\.(JPG|jpg)/) {photo ->

  Metadata metadata = JpegMetadataReader.readMetadata(photo);

  Directory exifDirectory = metadata.getDirectory(ExifDirectory.class)

  if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_ORIGINAL) && exifDirectory.containsTag(ExifDirectory.TAG_EXIF_IMAGE_HEIGHT) && exifDirectory.containsTag(ExifDirectory.TAG_EXIF_IMAGE_WIDTH)) {
    dateTimeOriginal = exifDirectory.getDate(ExifDirectory.TAG_DATETIME_ORIGINAL)

    // the problem here: the exif information does not necessarilly reflect the real dimension of the image ....
    // ... ACDSee for example does not update this information.
//    imageHeight = exifDirectory.getInt(ExifDirectory.TAG_EXIF_IMAGE_HEIGHT)
//    imageWidth = exifDirectory.getInt(ExifDirectory.TAG_EXIF_IMAGE_WIDTH)
  }

// *****
// Width and height using java imageio:
// *****

//This is why I asked the original poster if he/she was going to use the images later on. Technically speaking, you don't have to read the whole image to obtain the width and height information.

  ImageInputStream imageStream = ImageIO.createImageInputStream(photo)
  Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
  ImageReader reader = null;
  if(!readers.hasNext()) {
//     can't read image format!!! abort!!!
    throw new RuntimeException("Can't read image format.")
  }else {
    reader = readers.next();
  }
  reader.setInput(imageStream,true,true);

  imageWidth= reader.getWidth(0);
  imageHeight = reader.getHeight(0);

  reader.dispose();
  imageStream.close();



  String line = "Filename: ${photo.absolutePath}; Dimension: ${imageWidth} * ${imageHeight} ; Date: ${dateTimeOriginal}"
  println(line)
  writer.writeLine("${photo.name};${imageWidth};${imageHeight};${dateTimeOriginal.time}")
//
// Option 2: JAI
//

  // todo add switch for image processing method (java-JAI or imageMagick shellscript)
// Option 1: java and JAI
  // todo check if this works .... study jai once more.
// create display
//  ImagingHelper.createScaledImage(photo.name, photo, Math.max(imageHeight,imageWidth), displayPixelsLongerSide, displayFolder, true);
// create thumbnail
//  ImagingHelper.createScaledImage(photo.name, photo, Math.max(imageHeight, imageWidth), thumbnailPixelsLongerSide, thumbnailFolder, false);

  counter++
}
writer.close()

// todo call jhead -autorot



// todo switch for either imagemagick script or java down-scaling of images
//
// Option 2: Shell scripts and imagemagick:
// call shell script to create thumbs etc., use argument from groovy script as argument for the shell script
//
Process p = "/Users/alexanderjosef/scripts/sportrait/createSportraitImages.sh ${imageFineFolder.parent}".execute()
p.consumeProcessOutput(System.out, System.err)
p.waitFor()



// create a zip file with the name of the project's parent folder
def result = new ZipOutputStream(new FileOutputStream(new File(imageFineFolder.parentFile, imageFineFolder.parentFile.name + ".zip")))
// custom method for zipping a directory with thumbnail, displays and a import txt file

result.withStream {zipOutStream ->
  // put import.txt into zip
  println("zipping : " + importFile.path)
  zipOutStream.putNextEntry(new ZipEntry(importFile.name))
  new FileInputStream(importFile).withStream {inStream ->
    def buffer = new byte[1024]
    def count
    while ((count = inStream.read(buffer, 0, 1024)) != -1) {
      zipOutStream.write(buffer)
    }
  }
  zipOutStream.closeEntry()


  // zip thumbnail folder
  thumbnailFolder.eachFileMatch(~/.*\.(JPG|jpg)/) {f ->
    println("zipping : " + f.path)
    zipOutStream.putNextEntry(new ZipEntry(new File("thumbnail",f.name).getPath()))
    new FileInputStream(f).withStream {inStream ->
      def buffer = new byte[1024]
      def count
      while ((count = inStream.read(buffer, 0, 1024)) != -1) {
        zipOutStream.write(buffer)
      }
    }
    zipOutStream.closeEntry()

  }

  // zip display folder
  displayFolder.eachFileMatch(~/.*\.(JPG|jpg)/) {f ->
    println("zipping : " + f.path)
    zipOutStream.putNextEntry(new ZipEntry(new File("display",f.name).getPath()))
    new FileInputStream(f).withStream {inStream ->
      def buffer = new byte[1024]
      def count
      while ((count = inStream.read(buffer, 0, 1024)) != -1) {
        zipOutStream.write(buffer)
      }
    }
    zipOutStream.closeEntry()

  }

}

