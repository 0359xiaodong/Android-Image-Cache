package edu.mit.mobile.android.imagecache;
/*
 * Copyright (C) 2011  MIT Mobile Experience Lab
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

/**
 * A simple disk cache.
 *
 * @author <a href="mailto:spomeroy@mit.edu">Steve Pomeroy</a>
 *
 * @param <K> the key to store/retrieve the value
 * @param <V> the value that will be stored to disk
 */
public abstract class DiskCache<K, V> {
	private static final String TAG = "DiskCache";

	private MessageDigest hash;

	private final File mCacheBase;
	private final String mCachePrefix, mCacheSuffix;

	/**
	 * Creates a new disk cache with no cachePrefix or cacheSuffix
	 *
	 * @param cacheBase
	 */
	public DiskCache(File cacheBase) {
		this(cacheBase, null, null);
	}

	/**
	 * Creates a new disk cache.
	 *
	 * @param cacheBase The base directory within which all the cache files will be stored.
	 * @param cachePrefix If you want a prefix to the filenames, place one here. Otherwise, pass null.
	 * @param cacheSuffix A suffix to the cache filename. Null is also ok here.
	 */
	public DiskCache(File cacheBase, String cachePrefix, String cacheSuffix) {
		mCacheBase = cacheBase;
		mCachePrefix = cachePrefix;
		mCacheSuffix = cacheSuffix;

		try {
			hash = MessageDigest.getInstance("SHA-1");

		} catch (final NoSuchAlgorithmException e) {
			try {
				hash = MessageDigest.getInstance("MD5");
			} catch (final NoSuchAlgorithmException e2) {
			final RuntimeException re = new RuntimeException("No available hashing algorithm");
			re.initCause(e2);
			throw re;
			}
		}
	}

	/**
	 * Gets the cache filename for the given key.
	 *
	 * @param key
	 * @return
	 */
	protected File getFile(K key){
		return new File(mCacheBase,
				(mCachePrefix != null ? mCachePrefix :"" )
				+ hash(key)
				+ (mCacheSuffix  != null ? mCacheSuffix : "")
			);
	}

	/**
	 * Writes the value stored in the cache to disk by calling {@link #toDisk(Object, Object, OutputStream)}.
	 *
	 * @param key The key to find the value.
	 * @param value the data to be written to disk.
	 */
	public void put(K key, V value){
		final File saveHere = getFile(key);

		try {
			final OutputStream os = new FileOutputStream(saveHere);
			toDisk(key, value, os);
			os.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the value from disk using {@link #fromDisk(Object, InputStream)}.
	 *
	 * @param key
	 * @return The value for key or null if the key doesn't map to any existing entries.
	 */
	public V get(K key){
		final File readFrom = getFile(key);

		if (!readFrom.exists()){
			return null;
		}

		try {
			final InputStream is = new FileInputStream(readFrom);
			final V out = fromDisk(key, is);
			is.close();
			return out;

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		// XXX better exception handling
	}

	/**
	 * Clears the cache files from disk.
	 *
	 * Note: this only clears files that match the given prefix/suffix.
	 *
	 * @return true if the operation succeeded without error. It is possible that it will fail and the cache ends up being partially cleared.
	 */
	public boolean clear() {
		boolean success = true;

		for (final File cacheFile : mCacheBase.listFiles(mCacheFileFilter)){
			if (!cacheFile.delete()){
				// throw new IOException("cannot delete cache file");
				Log.e(TAG, "error deleting "+ cacheFile);
				success = false;
			}
		}
		return success;
	}

	/**
	 * @return the size of the cache as it is on disk.
	 */
	public int getCacheSize(){
		return mCacheBase.listFiles(mCacheFileFilter).length;
	}

	private final CacheFileFilter mCacheFileFilter = new CacheFileFilter();

	private class CacheFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			final String path = pathname.getName();
			return (mCachePrefix != null ? path.startsWith(mCachePrefix) : true)
				&& (mCacheSuffix != null ? path.endsWith(mCacheSuffix)   : true);
		}
	};

	/**
	 * Implement this to do the actual disk writing. Do not close the OutputStream; it will be closed for you.
	 *
	 * @param key
	 * @param in
	 * @param out
	 */
	protected abstract void toDisk(K key, V in, OutputStream out);

	/**
	 * Implement this to do the actual disk reading.
	 * @param key
	 * @param in
	 * @return a new instance of {@link V} containing the contents of in.
	 */
	protected abstract V fromDisk(K key, InputStream in);

	/**
	 * Using the key's {@link Object#toString() toString()} method, generates a string suitable for using as a filename.
	 *
	 * @param key
	 * @return a string uniquely representing the the key.
	 */
	public String hash(K key){
		hash.update(key.toString().getBytes());
		final byte[] ba = hash.digest();
		return new BigInteger(ba).toString(16);
	}
}
