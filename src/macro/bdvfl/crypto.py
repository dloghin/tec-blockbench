import base64
import hashlib
import secp256k1
import sha3

def genkey(idx):
    print("Generate private key for client {}".format(idx))
    pvk = secp256k1.PrivateKey()
    with open('client{}.pvk'.format(idx), mode='wb') as bfile:
        bfile.write(pvk.private_key)
        bfile.close()

def genkeys(num_clients):
    for i in range(num_clients):
        genkey(i)

def loadkey(idx):
    with open('client{}.pvk'.format(idx), mode='rb') as bfile:
        raw = bfile.read()
        bfile.close()
        pvk = secp256k1.PrivateKey(raw)
        pbk = pvk.pubkey
        str_pbk = base64.b64encode(pbk.serialize(compressed=False)).decode("ascii")
        return pvk, pbk, str_pbk

def sign_msg_test(msg):
    pvk, pbk, spbk = loadkey(0)
    bytes_object = bytes(msg, 'utf-8')
    with open('sig_msg_py.bin', mode='wb') as bfile:
        bfile.write(bytes_object)
        bfile.close()
    # sig = pvk.ecdsa_serialize(pvk.ecdsa_sign(bytes_object, raw=True, digest=sha3.keccak_256))
    sig = pvk.ecdsa_serialize_compact(pvk.ecdsa_sign(bytes_object))
    dig = hashlib.sha256(bytes_object).digest()
    with open('sig_sig_py.bin', mode='wb') as bfile:
        bfile.write(sig)
        bfile.close()
    with open('sig_dig_py.bin', mode='wb') as bfile:
        bfile.write(dig)
        bfile.close()
    print(spbk)
    # base64.b64encode(pbk.serialize(compressed=False)).decode("ascii")
    with open('sig_pbk_py.bin', mode='wb') as bfile:
        bfile.write(pbk.serialize(compressed=False))
        bfile.close()
    print(pbk.ecdsa_verify(bytes_object, pbk.ecdsa_deserialize_compact(sig)))
    print(base64.b64encode(sig).decode("ascii"))

def sign_msg(pvk, msg):
    bytes_object = bytes(msg, 'utf-8')
    sig = pvk.ecdsa_serialize_compact(pvk.ecdsa_sign(bytes_object))
    return base64.b64encode(sig).decode("ascii")

def loadkeys(num_clients):
    pvkeys = {}
    pbkeys = {}
    str_pbkeys = {}
    for i in range(num_clients):
        pvk, pbk, str_pbk = loadkey(i)
        pvkeys[i] = pvk
        pbkeys[i] = pbk
        str_pbkeys[i] = str_pbk
    return pvkeys, pbkeys, str_pbkeys

def get_hash(content):
    k = sha3.keccak_256()
    k.update(content)
    return k.hexdigest()

def get_hash_digest(content):
    k = sha3.keccak_256()
    k.update(content)
    return k.digest()[0]
