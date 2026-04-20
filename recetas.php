<?php
header('Content-Type: application/json');

$servername = "db";
$username = "admin";
$password = "test";
$dbname = "database";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    echo json_encode([
        "success" => false,
        "error" => "Connection failed"
    ]);
    exit;
}

$accion = $_POST["accion"] ?? "";

if ($accion === "insertar") {

    $titulo = $_POST["titulo"] ?? "";
    $categoria = $_POST["categoria"] ?? "";
    $tiempo = $_POST["tiempo"] ?? 0;
    $ingredientes = $_POST["ingredientes"] ?? "";
    $pasos = $_POST["pasos"] ?? "";
    $fotoUri = $_POST["fotoUri"] ?? "";
    $idUsuario = $_POST["idUsuario"] ?? 0;

    $sql = "INSERT INTO recetas (titulo, categoria, tiempo, ingredientes, pasos, fotoUri, favorita, idUsuario) VALUES (?, ?, ?, ?, ?, ?, 0, ?)";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ssisssi", $titulo, $categoria, $tiempo, $ingredientes, $pasos, $fotoUri, $idUsuario);

    if ($stmt->execute()) {
        echo json_encode([
            "success" => true,
            "id" => $stmt->insert_id
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "error" => $stmt->error
        ]);
    }

    $stmt->close();

} else if ($accion === "obtener") {

    $idUsuario = $_POST["idUsuario"] ?? 0;

    $sql = "SELECT id, titulo, categoria, tiempo, ingredientes, pasos, fotoUri, favorita FROM recetas WHERE idUsuario=?";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("i", $idUsuario);
    $stmt->execute();

    $result = $stmt->get_result();

    $recetas = [];

    while ($row = $result->fetch_assoc()) {
        $recetas[] = $row;
    }

    echo json_encode([
        "success" => true,
        "recetas" => $recetas
    ]);

    $stmt->close();

} else if ($accion === "eliminar") {

    $id = $_POST["id"] ?? 0;
    $idUsuario = $_POST["idUsuario"] ?? 0;


    $stmt = $conn->prepare("SELECT fotoUri FROM recetas WHERE id=? AND idUsuario=?");
    $stmt->bind_param("ii", $id, $idUsuario);
    $stmt->execute();
    $res = $stmt->get_result();
    $row = $res->fetch_assoc();
    $stmt->close();

    if ($row && !empty($row['fotoUri'])) {
        $ruta = __DIR__ . "/" . $row['fotoUri'];
        if (file_exists($ruta)) {
            unlink($ruta);
        }
    }

    $sql = "DELETE FROM recetas WHERE id=? AND idUsuario=?";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ii", $id, $idUsuario);

    if ($stmt->execute()) {
        echo json_encode(["success" => true]);
    } else {
        echo json_encode([
            "success" => false,
            "error" => $stmt->error
        ]);
        exit;
    }

    $stmt->close();

} else if ($accion === "favorito") {

    $id = $_POST["id"] ?? 0;
    $idUsuario = $_POST["idUsuario"] ?? 0;
    $favorita = $_POST["favorita"] ?? 0;

    $sql = "UPDATE recetas SET favorita=? WHERE id=? AND idUsuario=?";

    $stmt = $conn->prepare($sql);
    $stmt->bind_param("iii", $favorita, $id, $idUsuario);

    if ($stmt->execute()) {
        echo json_encode(["success" => true]);
    } else {
        echo json_encode([
            "success" => false,
            "error" => $stmt->error
        ]);
    }

    $stmt->close();

} else {

    echo json_encode([
        "success" => false,
        "error" => "Acción no válida"
    ]);
}

$conn->close();
?>
