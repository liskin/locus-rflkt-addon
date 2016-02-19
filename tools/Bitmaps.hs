import Data.Bits
import Data.List.Split (chunksOf)
import Data.Word (Word8)
import qualified Data.ByteString as B
import qualified Data.ByteString.Base64 as B64
import qualified Data.ByteString.Char8 as BC8

decode :: Int -> B.ByteString -> [[Word8]]
decode width =
    chunksOf width . concatMap f . B.unpack . either error id . B64.decode
    where
        f x = [x .&. 0x3, x `shiftR` 2 .&. 0x3, x `shiftR` 4 .&. 0x3, x `shiftR` 6 .&. 0x3]

encode :: [[Word8]] -> B.ByteString
encode = B64.encode . B.pack . map f . chunksOf 4 . concat
    where
        f [a, b, c, d] = (((((d `shiftL` 2) .|. c) `shiftL` 2) .|. b) `shiftL` 2) .|. a

printBitmap :: [[Word8]] -> IO ()
printBitmap = mapM_ putStrLn . map (map f)
    where
        f 0 = '█'
        f 1 = '▒'
        f 2 = '░'
        f 3 = ' '

-- assumes output from pnmtoplainpnm
fromPbm :: String -> [[Word8]]
fromPbm = map (map f) . drop 2 . lines
    where
        f '0' = 3
        f '1' = 0

-- xbmtopbm file.xbm | pnmtoplainpnm | runhaskell tools/Bitmaps.hs
main :: IO ()
main = do
    pbm <- getContents
    BC8.putStrLn $ encode $ fromPbm pbm
