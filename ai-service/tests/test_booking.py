import pytest

from booking_engine import calculer_prix_chambre


def test_standard_room_in_low_season_without_sea_view():
    assert calculer_prix_chambre("standard", "basse", False, 3) == 300.0


def test_sea_view_adds_nightly_supplement():
    assert calculer_prix_chambre("standard", "basse", True, 3) == 360.0


def test_suite_in_high_season_with_sea_view_and_long_stay():
    assert calculer_prix_chambre("suite", "haute", True, 10) == 3645.0


def test_seven_nights_do_not_get_long_stay_discount():
    assert calculer_prix_chambre("suite", "basse", False, 7) == 1750.0


@pytest.mark.parametrize("duration", [0, -1])
def test_non_positive_duration_is_rejected(duration):
    with pytest.raises(
        ValueError, match="La durée du séjour doit être positive."
    ):
        calculer_prix_chambre("standard", "basse", False, duration)


def test_unknown_room_type_is_rejected():
    with pytest.raises(ValueError, match="Type de chambre inconnu."):
        calculer_prix_chambre("deluxe", "basse", False, 1)


def test_unknown_season_is_rejected():
    with pytest.raises(ValueError, match="Saison inconnue."):
        calculer_prix_chambre("standard", "moyenne", False, 1)
