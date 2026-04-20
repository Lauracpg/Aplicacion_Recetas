<?php
header('Content-Type: application/json');

$conn = new mysqli("db", "admin", "test", "database");

if ($conn->connect_error) {
    die(json_encode(["success"=>false,"error"=>"DB error"]));
}

$tipo = $_POST['tipo'] ?? '';
$imagen = $_POST['imagen'] ?? '';

if (!$tipo || !$imagen) {
    echo json_encode(["success"=>false,"error"=>"Datos incompletos"]);
    exit;
}

$carpeta = __DIR__ . "/uploads/";
if (!file_exists($carpeta)) {
    mkdir($carpeta, 0777, true);
}

$imagenDecodificada = base64_decode($imagen);

if ($imagenDecodificada === false) {
    echo json_encode(["success"=>false,"error"=>"Error base64"]);
    exit;
}

if ($tipo === "perfil") {

    $email = $_POST['email'] ?? '';
    if (!$email) {
        echo json_encode(["success"=>false,"error"=>"Email requerido"]);
        exit;
    }

    $emailSeguro = preg_replace('/[^a-zA-Z0-9]/', '_', $email);
    $nombreArchivo = $carpeta . $emailSeguro . ".jpg";
    $rutaRelativa = "uploads/" . $emailSeguro . ".jpg";

    file_put_contents($nombreArchivo, $imagenDecodificada);

    $stmt = $conn->prepare("UPDATE usuarios SET foto=? WHERE email=?");
    $stmt->bind_param("ss", $rutaRelativa, $email);

    if (!$stmt->execute()) {
        echo json_encode(["success"=>false,"error"=>"DB error"]);
        exit;
    }

    echo json_encode(["success"=>true,"ruta"=>$rutaRelativa]);

} else if ($tipo === "receta") {

    $imagen = $_POST['imagen'] ?? '';
    $idUsuario = $_POST['idUsuario'] ?? 0;

    if (!$imagen) {
        echo json_encode(["success"=>false,"error"=>"imagen requerida"]);
        exit;
    }

    $imagenDecodificada = base64_decode($imagen);

    if ($imagenDecodificada === false) {
        echo json_encode(["success"=>false,"error"=>"Error base64"]);
        exit;
    }

    $idReceta = $_POST['id'] ?? null;
    $imagenId = $_POST['imagenId'] ?? null;

    if ($imagenId && !$idReceta) {

        $nombreArchivo = $carpeta . $imagenId . ".jpg";
        $rutaRelativa = "uploads/" . $imagenId . ".jpg";

        file_put_contents($nombreArchivo, $imagenDecodificada);

        echo json_encode([
            "success" => true,
            "ruta" => $rutaRelativa
        ]);
        exit;
    }

    if ($idReceta) {

        $nombreArchivo = $carpeta . "receta_" . $idReceta . ".jpg";
        $rutaRelativa = "uploads/receta_" . $idReceta . ".jpg";

        file_put_contents($nombreArchivo, $imagenDecodificada);

        $stmt = $conn->prepare(
            "UPDATE recetas SET fotoUri=? WHERE id=? AND idUsuario=?"
        );

        $stmt->bind_param("sii", $rutaRelativa, $idReceta, $idUsuario);

        if (!$stmt->execute()) {
            echo json_encode(["success"=>false,"error"=>"DB error"]);
            exit;
        }

        echo json_encode([
            "success" => true,
            "ruta" => $rutaRelativa
        ]);
        exit;
    }

    echo json_encode([
        "success" => false,
        "error" => "Falta imagenId o id"
    ]);
}

$conn->close();
?>
